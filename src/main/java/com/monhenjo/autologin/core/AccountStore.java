package com.monhenjo.autologin.core;

import java.util.List;
import java.util.Optional;

/**
 * The accounts a player has saved.
 *
 * <p>Small on purpose: the picker, the config screen and the commands all go through these six
 * calls, and the encrypted file behind them never leaks into the UI. Persistence happens inside
 * the implementation, so a caller cannot forget to save (or accidentally save a half-edited list).
 */
public interface AccountStore {
	/** Every saved account, most recently used first. */
	List<Account> all();

	/** Accounts offered for a server: exact address matches and wildcard entries. */
	List<Account> forServer(String serverKey);

	Optional<Account> find(String id);

	/**
	 * @throws IllegalArgumentException if the password cannot be sent as a chat command
	 */
	Account add(String label, String password, String serverKey);

	void update(Account account);

	boolean remove(String id);

	void markUsed(String id, long epochMillis);

	/** Why the store is currently unable to persist, if it is. */
	Optional<String> problem();
}
