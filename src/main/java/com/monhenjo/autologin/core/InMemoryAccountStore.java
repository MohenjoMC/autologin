package com.monhenjo.autologin.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link AccountStore}: used by the unit tests, and as the fallback when the vault
 * cannot be opened so the picker still works for the current session.
 */
public final class InMemoryAccountStore implements AccountStore {
	private final List<Account> accounts = new ArrayList<>();
	private final String problem;

	public InMemoryAccountStore() {
		this(null);
	}

	public InMemoryAccountStore(String problem) {
		this.problem = problem;
	}

	@Override
	public List<Account> all() {
		List<Account> copy = new ArrayList<>(accounts);
		copy.sort(Comparator.comparingLong(Account::lastUsedAt).reversed());
		return copy;
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
		return accounts.stream().filter(account -> account.id().equals(id)).findFirst();
	}

	@Override
	public Account add(String label, String password, String serverKey) {
		if (!LoginVerbs.isValidPassword(password)) {
			throw new IllegalArgumentException("password cannot be sent as a chat command");
		}

		Account account = Account.create(label, password, serverKey);
		accounts.add(account);
		return account;
	}

	@Override
	public void update(Account account) {
		for (int i = 0; i < accounts.size(); i++) {
			if (accounts.get(i).id().equals(account.id())) {
				accounts.set(i, account);
				return;
			}
		}

		accounts.add(account);
	}

	@Override
	public boolean remove(String id) {
		return accounts.removeIf(account -> account.id().equals(id));
	}

	@Override
	public void markUsed(String id, long epochMillis) {
		for (int i = 0; i < accounts.size(); i++) {
			Account account = accounts.get(i);
			if (account.id().equals(id)) {
				accounts.set(i, account.withLastUsed(epochMillis, account.uses() + 1));
				return;
			}
		}
	}

	@Override
	public Optional<String> problem() {
		return Optional.ofNullable(problem);
	}
}
