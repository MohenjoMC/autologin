package com.monhenjo.autologin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Picks which saved login to use, and how to spell it.
 *
 * <p>Separate from {@link LoginFlow} because it answers a different question: the flow knows
 * whether a picker should appear, this decides which account an automatic login should use.
 * "Automatic" still means "the account the player nominated", never "guess".
 */
public final class AutoLoginChoice {
	private AutoLoginChoice() {
	}

	/**
	 * @param preferred label the player pinned in the config, may be blank
	 * @param options   the accounts offered for this server, best first
	 */
	public static Optional<Account> choose(String preferred, List<Account> options) {
		if (options == null || options.isEmpty()) {
			return Optional.empty();
		}

		String wanted = preferred == null ? "" : preferred.trim().toLowerCase(Locale.ROOT);
		if (!wanted.isEmpty()) {
			for (Account account : options) {
				if (account.label().toLowerCase(Locale.ROOT).equals(wanted)) {
					return Optional.of(account);
				}
			}
		}

		return options.stream().filter(Account::usable).findFirst();
	}

	/**
	 * Whether a typed argument is one of the saved labels rather than a password. Suggestions
	 * offer labels, so {@code /login test} must resolve to the saved password, and
	 * {@code /login hunter2} must go to the server untouched.
	 */
	public static Optional<Account> matchLabel(String typed, List<Account> options) {
		if (typed == null || typed.isBlank() || options == null) {
			return Optional.empty();
		}

		String wanted = typed.trim().toLowerCase(Locale.ROOT);
		for (Account account : options) {
			if (account.label().toLowerCase(Locale.ROOT).equals(wanted)) {
				return Optional.of(account);
			}
		}

		return Optional.empty();
	}

	/** Labels that start with the typed text, for the command suggestion list. */
	public static List<String> suggest(String typed, List<Account> options) {
		String prefix = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
		List<String> labels = new ArrayList<>();

		if (options == null) {
			return labels;
		}

		for (Account account : options) {
			if (account.label().toLowerCase(Locale.ROOT).startsWith(prefix)) {
				labels.add(account.label());
			}
		}

		return labels;
	}
}
