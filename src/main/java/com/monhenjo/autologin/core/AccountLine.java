package com.monhenjo.autologin.core;

import java.util.Locale;
import java.util.Optional;

/**
 * One saved login as it is written in the config file: {@code label|server|password}.
 *
 * <p>Kept in {@code core} (and tested) because it is the only place a pipe in a password can be
 * lost: the first two pipes separate the fields, everything after the second belongs to the
 * password. A password with a pipe is legal, a label with one is not - the label is what the
 * player types after {@code /login}.
 */
public record AccountLine(String label, String server, String password) {
	private static final char SEPARATOR = '|';

	public AccountLine {
		label = label == null ? "" : label.trim();
		server = server == null ? "" : server.trim();
		password = password == null ? "" : password;
	}

	public static String format(String label, String server, String password) {
		return new AccountLine(label, server, password).toString();
	}

	public static Optional<AccountLine> parse(String line) {
		if (line == null || line.isBlank()) {
			return Optional.empty();
		}

		int first = line.indexOf(SEPARATOR);
		int second = first < 0 ? -1 : line.indexOf(SEPARATOR, first + 1);
		if (second < 0) {
			return Optional.empty();
		}

		AccountLine parsed = new AccountLine(
			line.substring(0, first),
			line.substring(first + 1, second),
			line.substring(second + 1)
		);

		return parsed.isUsable() ? Optional.of(parsed) : Optional.empty();
	}

	/** A line nobody can log in with is noise in the UI, not a profile. */
	public boolean isUsable() {
		return !label.isEmpty() && LoginVerbs.isValidPassword(password);
	}

	/** The server this line applies to, or {@link ServerKey#ANY} for every server. */
	public String serverKey() {
		return server.isEmpty() ? ServerKey.ANY : ServerKey.normalize(server);
	}

	public Account toAccount() {
		return new Account(
			label,
			label,
			password,
			serverKey(),
			System.currentTimeMillis(),
			0L,
			0
		);
	}

	@Override
	public String toString() {
		return label + SEPARATOR + server + SEPARATOR + password;
	}
}
