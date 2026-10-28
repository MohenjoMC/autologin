package com.monhenjo.autologin.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.AccountStore;
import com.monhenjo.autologin.core.LoginVerbs;
import com.monhenjo.autologin.core.ServerKey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The on-disk account store: one sealed JSON blob under {@code config/autologin/}.
 *
 * <p>Failure is expected and handled rather than fatal, because the vault key is tied to the
 * machine. A file that cannot be decrypted (copy of a config folder, reinstalled OS, different
 * user) leaves the store empty and read-only, with a message for the settings screen. It is never
 * overwritten while unreadable: silently replacing someone's saved logins with an empty list would
 * be the worst possible bug in this mod.
 */
public final class VaultAccountStore implements AccountStore {
	private static final String FORMAT_KEY = "format";
	private static final String PAYLOAD_KEY = "payload";
	private static final int FORMAT = 1;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path file;
	private final Vault vault;
	private final List<Account> accounts = new ArrayList<>();

	private String problem;

	private VaultAccountStore(Path file, Vault vault) {
		this.file = file;
		this.vault = vault;
	}

	/**
	 * Opens the store in {@code directory}, creating it if needed. Never throws: a store that
	 * cannot read or write reports the reason from {@link #problem()} and keeps working in memory
	 * for the session.
	 */
	public static VaultAccountStore open(Path directory) {
		Path file = directory.resolve("accounts.vault");
		VaultAccountStore store;

		try {
			Files.createDirectories(directory);
			Vault vault = Vault.forLocalMachine(directory.resolve("vault.salt"));
			store = new VaultAccountStore(file, vault);
		} catch (IOException | RuntimeException e) {
			store = new VaultAccountStore(file, null);
			store.problem = "saved logins are unavailable: " + e.getMessage();
			return store;
		}

		store.load();
		return store;
	}

	private void load() {
		if (!Files.exists(file)) {
			return;
		}

		try {
			String envelope = Files.readString(file, StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(envelope).getAsJsonObject();

			int format = root.has(FORMAT_KEY) ? root.get(FORMAT_KEY).getAsInt() : FORMAT;
			if (format != FORMAT) {
				problem = "saved logins were written by a newer AutoLogin (format " + format + ")";
				return;
			}

			byte[] sealed = Base64.getDecoder().decode(root.get(PAYLOAD_KEY).getAsString());
			byte[] plain = vault.open(sealed);
			readPayload(new String(plain, StandardCharsets.UTF_8));
		} catch (Vault.VaultException e) {
			problem = "saved logins cannot be decrypted on this machine";
		} catch (IOException | RuntimeException e) {
			problem = "saved logins could not be read: " + e.getMessage();
		}
	}

	private void readPayload(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonArray list = root.has("accounts") ? root.getAsJsonArray("accounts") : new JsonArray();

		for (JsonElement element : list) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject object = element.getAsJsonObject();
			String password = stringOr(object, "password", "");
			if (!LoginVerbs.isValidPassword(password)) {
				// Written by hand, or a password the chat command could not carry. Keep the entry
				// out of the picker rather than offering something that cannot be sent.
				continue;
			}

			accounts.add(new Account(
				stringOr(object, "id", null),
				stringOr(object, "label", ""),
				password,
				ServerKey.normalize(stringOr(object, "server", ServerKey.ANY)),
				numberOr(object, "createdAt", 0L),
				numberOr(object, "lastUsedAt", 0L),
				(int) numberOr(object, "uses", 0L)
			));
		}
	}

	@Override
	public synchronized List<Account> all() {
		List<Account> copy = new ArrayList<>(accounts);
		copy.sort(Comparator.comparingLong(Account::lastUsedAt).reversed());
		return copy;
	}

	@Override
	public synchronized List<Account> forServer(String serverKey) {
		List<Account> matching = new ArrayList<>();
		for (Account account : all()) {
			if (ServerKey.matches(account.serverKey(), serverKey)) {
				matching.add(account);
			}
		}
		return matching;
	}

	@Override
	public synchronized Optional<Account> find(String id) {
		return accounts.stream().filter(account -> account.id().equals(id)).findFirst();
	}

	@Override
	public synchronized Account add(String label, String password, String serverKey) {
		if (!LoginVerbs.isValidPassword(password)) {
			throw new IllegalArgumentException("passwords cannot contain spaces or line breaks");
		}

		Account account = Account.create(label, password, serverKey);
		accounts.add(account);
		save();
		return account;
	}

	@Override
	public synchronized void update(Account account) {
		for (int i = 0; i < accounts.size(); i++) {
			if (accounts.get(i).id().equals(account.id())) {
				accounts.set(i, account);
				save();
				return;
			}
		}

		accounts.add(account);
		save();
	}

	@Override
	public synchronized boolean remove(String id) {
		boolean removed = accounts.removeIf(account -> account.id().equals(id));
		if (removed) {
			save();
		}

		return removed;
	}

	@Override
	public synchronized void markUsed(String id, long epochMillis) {
		for (int i = 0; i < accounts.size(); i++) {
			Account account = accounts.get(i);
			if (account.id().equals(id)) {
				accounts.set(i, account.withLastUsed(epochMillis, account.uses() + 1));
				save();
				return;
			}
		}
	}

	@Override
	public synchronized Optional<String> problem() {
		return Optional.ofNullable(problem);
	}

	/** Writes the vault. Refuses to write while unreadable, so a failed read cannot destroy data. */
	private void save() {
		if (problem != null || vault == null) {
			return;
		}

		try {
			String payload = toPayload();
			byte[] sealed = vault.seal(payload.getBytes(StandardCharsets.UTF_8));

			JsonObject root = new JsonObject();
			root.addProperty(FORMAT_KEY, FORMAT);
			root.addProperty(PAYLOAD_KEY, Base64.getEncoder().encodeToString(sealed));

			Path parent = file.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
			Vault.restrictToOwner(temp);

			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException | RuntimeException e) {
			problem = "saved logins could not be written: " + e.getMessage();
		}
	}

	private String toPayload() {
		JsonArray list = new JsonArray();

		for (Account account : accounts) {
			JsonObject object = new JsonObject();
			object.addProperty("id", account.id());
			object.addProperty("label", account.label());
			object.addProperty("password", account.password());
			object.addProperty("server", account.serverKey());
			object.addProperty("createdAt", account.createdAt());
			object.addProperty("lastUsedAt", account.lastUsedAt());
			object.addProperty("uses", account.uses());
			list.add(object);
		}

		JsonObject root = new JsonObject();
		root.add("accounts", list);
		return GSON.toJson(root);
	}

	private static String stringOr(JsonObject object, String key, String fallback) {
		return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
	}

	private static long numberOr(JsonObject object, String key, long fallback) {
		try {
			return object.has(key) ? object.get(key).getAsLong() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}
}
