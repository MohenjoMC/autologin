package com.monhenjo.autologin.client.config;

import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.AccountStore;
import com.monhenjo.autologin.core.LoginVerbs;
import com.monhenjo.autologin.core.ServerKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Saved logins, stored as the entries of the Cloth Config screen.
 *
 * <p>The screen's list *is* the database: what the player edits is what the mod uses. Entries are
 * objects with a field per value, so nothing has to be parsed out of a packed string.
 */
public final class ConfigAccountStore implements AccountStore {
	private final AutoLoginConfig config;

	/** Recency for ordering only; not worth a field in the file. */
	private final Map<String, Long> lastUsed = new HashMap<>();

	public ConfigAccountStore(AutoLoginConfig config) {
		this.config = config;
	}

	@Override
	public List<Account> all() {
		List<Account> accounts = new ArrayList<>();

		for (AutoLoginConfig.LoginEntry entry : entries()) {
			if (entry == null || entry.label == null || entry.label.isBlank()) {
				continue;
			}

			accounts.add(new Account(entry.label.trim(), entry.label.trim(), entry.password,
				ServerKey.normalize(entry.server), System.currentTimeMillis(), 0L, 0));
		}

		accounts.sort(Comparator.comparingLong((Account account) -> lastUsed.getOrDefault(account.id(), 0L)).reversed());
		return accounts;
	}

	@Override
	public List<Account> forServer(String serverKey) {
		List<Account> matching = new ArrayList<>();

		for (Account account : all()) {
			if (ServerKey.matches(account.serverKey(), serverKey)) {
				matching.add(account);
			}
		}

		return matching;
	}

	@Override
	public Optional<Account> find(String id) {
		return all().stream().filter(account -> account.id().equals(id)).findFirst();
	}

	@Override
	public Account add(String label, String password, String server) {
		if (!LoginVerbs.isValidPassword(password)) {
			throw new IllegalArgumentException("passwords cannot contain spaces or line breaks");
		}

		String clean = label == null || label.isBlank() ? "main" : label.trim();
		remove(clean);

		Account account = new Account(clean, clean, password, ServerKey.normalize(server), System.currentTimeMillis(), 0L, 0);
		entries().add(new AutoLoginConfig.LoginEntry(clean, account.serverKey(), password));
		save();
		return account;
	}

	@Override
	public void update(Account account) {
		List<AutoLoginConfig.LoginEntry> entries = entries();

		for (AutoLoginConfig.LoginEntry entry : entries) {
			if (entry != null && entry.label != null && entry.label.equalsIgnoreCase(account.label())) {
				entry.label = account.label();
				entry.server = account.serverKey();
				entry.password = account.password();
				save();
				return;
			}
		}

		entries.add(new AutoLoginConfig.LoginEntry(account.label(), account.serverKey(), account.password()));
		save();
	}

	@Override
	public boolean remove(String id) {
		String wanted = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
		boolean removed = entries().removeIf(entry ->
			entry != null && entry.label != null && entry.label.trim().toLowerCase(Locale.ROOT).equals(wanted));

		if (removed) {
			save();
		}

		return removed;
	}

	@Override
	public void markUsed(String id, long epochMillis) {
		lastUsed.put(id, epochMillis);
	}

	@Override
	public Optional<String> problem() {
		return Optional.empty();
	}

	/** The live list behind the Cloth screen. */
	private List<AutoLoginConfig.LoginEntry> entries() {
		if (config.logins == null) {
			config.logins = new ArrayList<>();
		}

		return config.logins;
	}

	private void save() {
		Configs.save();
	}
}
