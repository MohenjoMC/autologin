package com.monhenjo.autologin.client.config;

import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.AccountLine;
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
 * Saved logins, stored as the {@code label|server|password} rows of the Cloth Config screen.
 *
 * <p>The point of this class is that the config file *is* the database: what the player sees in
 * the list is what the mod uses, with no second store to drift out of sync. That also means the
 * passwords are only as safe as the config file - it is created owner-readable where the
 * filesystem supports it, and the screen says so out loud.
 */
public final class ConfigAccountStore implements AccountStore {
	private final AutoLoginConfig config;

	/** Recency for ordering only; not worth a column in the file. */
	private final Map<String, Long> lastUsed = new HashMap<>();

	public ConfigAccountStore(AutoLoginConfig config) {
		this.config = config;
	}

	@Override
	public List<Account> all() {
		List<Account> accounts = new ArrayList<>();

		for (String line : rows()) {
			AccountLine.parse(line).ifPresent(parsed -> accounts.add(parsed.toAccount()));
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

		String clean = label == null || label.isBlank() ? "main" : label.trim().replace("|", "");
		if (clean.isEmpty()) {
			throw new IllegalArgumentException("a login needs a label");
		}

		remove(clean);

		Account account = new Account(clean, clean, password, ServerKey.normalize(server), System.currentTimeMillis(), 0L, 0);
		rows().add(AccountLine.format(clean, account.serverKey(), password));
		save();
		return account;
	}

	@Override
	public void update(Account account) {
		List<String> rows = rows();

		for (int i = 0; i < rows.size(); i++) {
			Optional<AccountLine> existing = AccountLine.parse(rows.get(i));
			if (existing.isPresent() && existing.get().label().equalsIgnoreCase(account.label())) {
				rows.set(i, AccountLine.format(account.label(), account.serverKey(), account.password()));
				save();
				return;
			}
		}

		rows.add(AccountLine.format(account.label(), account.serverKey(), account.password()));
		save();
	}

	@Override
	public boolean remove(String id) {
		String wanted = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
		List<String> rows = rows();
		boolean removed = rows.removeIf(row -> AccountLine.parse(row)
			.map(line -> line.label().toLowerCase(Locale.ROOT).equals(wanted))
			.orElse(false));

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

	/** The live list behind the Cloth screen; edits here show up on the next save. */
	private List<String> rows() {
		if (config.accounts == null) {
			config.accounts = new ArrayList<>();
		}

		return config.accounts;
	}

	private void save() {
		Configs.save();
	}
}
