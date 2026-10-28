package com.monhenjo.autologin.core;

import java.util.Locale;

/**
 * The identity of a server, reduced to something that survives the ways users type addresses.
 *
 * <p>{@code mc.example.com}, {@code MC.Example.com:25565} and {@code mc.example.com:25565} are
 * the same server; {@code mc.example.com:25566} is not.
 */
public final class ServerKey {
	/** Accounts may be marked to apply to any server. */
	public static final String ANY = "*";

	public static final int DEFAULT_PORT = 25565;

	private ServerKey() {
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}

		String key = raw.trim().toLowerCase(Locale.ROOT);
		if (key.isEmpty()) {
			return "";
		}

		int scheme = key.indexOf("://");
		if (scheme >= 0) {
			key = key.substring(scheme + 3);
		}

		int slash = key.indexOf('/');
		if (slash >= 0) {
			key = key.substring(0, slash);
		}

		if (key.endsWith(".")) {
			key = key.substring(0, key.length() - 1);
		}

		if (key.startsWith("[")) {
			// IPv6 literal, optionally with a port: [::1]:25565
			int close = key.indexOf(']');
			if (close > 0) {
				String host = key.substring(0, close + 1);
				String port = key.length() > close + 2 && key.charAt(close + 1) == ':'
					? key.substring(close + 2)
					: "";
				return applyPort(host, port);
			}

			return key;
		}

		int colon = key.lastIndexOf(':');
		if (colon > 0) {
			return applyPort(key.substring(0, colon), key.substring(colon + 1));
		}

		return key;
	}

	private static String applyPort(String host, String port) {
		if (port.isEmpty() || String.valueOf(DEFAULT_PORT).equals(port)) {
			return host;
		}

		return host + ":" + port;
	}

	/** Whether an account saved for {@code accountKey} is offered on {@code serverKey}. */
	public static boolean matches(String accountKey, String serverKey) {
		String account = normalize(accountKey);
		if (account.isEmpty() || ANY.equals(account)) {
			return true;
		}

		return account.equals(normalize(serverKey));
	}

	/** Best-effort label for the UI: the host without the default port. */
	public static String display(String serverKey) {
		String key = normalize(serverKey);
		return key.isEmpty() ? "this server" : key;
	}
}
