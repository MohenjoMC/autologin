package com.monhenjo.autologin.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Remembers the password of a registration the player typed by hand, so it can be saved once the
 * server confirms the registration.
 *
 * <p>Nothing is written on the strength of the command alone: a registration can fail (nickname
 * taken, password rejected, timeout), so the password is only kept for a short window and only
 * used when the server's own success message arrives.
 */
public final class RegisterCapture {
	/** Long enough for a slow plugin, short enough that a stale password cannot be saved later. */
	public static final long WINDOW_MILLIS = 30_000L;

	private final long windowMillis;
	private Captured pending;

	public RegisterCapture() {
		this(WINDOW_MILLIS);
	}

	public RegisterCapture(long windowMillis) {
		this.windowMillis = Math.max(1_000L, windowMillis);
	}

	/**
	 * Feeds a command the player sent. Only registrations with a matching confirmation count:
	 * {@code /register pass pass}, {@code /reg pass}, {@code /register pass}.
	 *
	 * @return true when this was a registration we are willing to remember
	 */
	public boolean onCommand(String serverKey, String commandLine, long now) {
		Optional<String> password = passwordOf(commandLine);
		if (password.isEmpty()) {
			return false;
		}

		pending = new Captured(password.get(), ServerKey.normalize(serverKey), now);
		return true;
	}

	/**
	 * The password to save, if a registration was started recently and the server has just said
	 * it succeeded. Consumes the capture, so a repeated success message cannot save twice.
	 */
	public Optional<Captured> consumeIfRecent(long now) {
		Captured captured = pending;
		if (captured == null) {
			return Optional.empty();
		}

		pending = null;

		if (now - captured.at() > windowMillis) {
			return Optional.empty();
		}

		return Optional.of(captured);
	}

	public void clear() {
		pending = null;
	}

	public Optional<Captured> pending() {
		return Optional.ofNullable(pending);
	}

	/**
	 * Extracts the password from a registration command line, or empty when the line is not a
	 * registration or its two arguments disagree (a typo is not something to save).
	 */
	public static Optional<String> passwordOf(String commandLine) {
		if (commandLine == null) {
			return Optional.empty();
		}

		String line = commandLine.trim();
		if (line.startsWith("/")) {
			line = line.substring(1);
		}

		String[] parts = line.split("\\s+");
		if (parts.length < 2 || !LoginVerbs.isRegisterVerb(parts[0])) {
			return Optional.empty();
		}

		String password = parts[1];
		if (!LoginVerbs.isValidPassword(password)) {
			return Optional.empty();
		}

		if (parts.length >= 3 && !password.equals(parts[2])) {
			return Optional.empty();
		}

		return Optional.of(password);
	}

	/** A label that reads like the player's own account rather than "main2". */
	public static String labelFor(String playerName, java.util.List<Account> existing) {
		String base = playerName == null ? "" : playerName.trim();
		if (base.isEmpty()) {
			base = "main";
		}

		String candidate = base;
		int suffix = 2;

		while (taken(candidate, existing)) {
			candidate = base + suffix;
			suffix++;
		}

		return candidate;
	}

	private static boolean taken(String label, java.util.List<Account> existing) {
		if (existing == null) {
			return false;
		}

		String wanted = label.toLowerCase(Locale.ROOT);
		return existing.stream().anyMatch(account -> account.label().toLowerCase(Locale.ROOT).equals(wanted));
	}

	public record Captured(String password, String serverKey, long at) {
	}
}
