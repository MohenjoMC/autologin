package com.monhenjo.autologin.core;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The only place a password is ever turned into something that leaves the client.
 *
 * <p>Two rules keep this safe, and both are enforced here rather than at the call sites:
 *
 * <ol>
 *   <li>A password is only ever sent as a command. Never as chat. Chat is broadcast to
 *       every player on the server, so a chat-based path would hand the password to the
 *       whole lobby.</li>
 *   <li>The command verb must be one of a short allowlist. A server can print any text it
 *       likes, including "please run /msg Notch &lt;your password&gt;", so the verb is never
 *       taken from the message unless it is a known authentication command.</li>
 * </ol>
 */
public final class LoginVerbs {
	/** Verbs servers actually use for chat-based authentication. */
	private static final Set<String> ALLOWED = Set.of(
		"login", "log", "l", "auth", "authenticate", "in",
		"register", "reg", "signup", "sign_up"
	);

	private static final Pattern SLASH_VERB = Pattern.compile("/([A-Za-z][A-Za-z0-9_]{0,15})\\b");

	private LoginVerbs() {
	}

	public static boolean isAllowed(String verb) {
		return verb != null && ALLOWED.contains(verb.toLowerCase(Locale.ROOT));
	}

	/** The first allowlisted {@code /verb} mentioned in a server message, if any. */
	public static Optional<String> find(String text) {
		if (text == null || text.isEmpty()) {
			return Optional.empty();
		}

		Matcher matcher = SLASH_VERB.matcher(text);
		while (matcher.find()) {
			String verb = matcher.group(1).toLowerCase(Locale.ROOT);
			if (ALLOWED.contains(verb)) {
				return Optional.of(verb);
			}
		}

		return Optional.empty();
	}

	/**
	 * Picks the verb for a prompt: whatever the server itself printed if it is allowlisted,
	 * otherwise the rule's default, otherwise the kind's usual command.
	 */
	public static String resolve(String text, LoginKind kind, String ruleDefault) {
		Optional<String> fromMessage = find(text);
		if (fromMessage.isPresent()) {
			return fromMessage.get();
		}

		if (isAllowed(ruleDefault)) {
			return ruleDefault.toLowerCase(Locale.ROOT);
		}

		return kind == LoginKind.REGISTER ? "register" : "login";
	}

	/**
	 * Rejects passwords that cannot survive a chat command. Auth plugins split on spaces, so a
	 * password with a space is unauthenticatable; a newline would let a stored value append a
	 * second command.
	 */
	public static boolean isValidPassword(String password) {
		if (password == null || password.isEmpty()) {
			return false;
		}

		for (int i = 0; i < password.length(); i++) {
			char c = password.charAt(i);
			if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || Character.isISOControl(c)) {
				return false;
			}
		}

		return true;
	}

	/** The full command line to hand to {@code ClientPacketListener#sendCommand}. */
	public static String build(String verb, String password) {
		return build(verb, password, 1);
	}

	/**
	 * @param repeats how many times the password belongs on the line: 1 for
	 *                {@code /login <password>}, 2 for {@code /register <password> <password>}
	 */
	public static String build(String verb, String password, int repeats) {
		if (!isAllowed(verb)) {
			throw new IllegalArgumentException("refusing to send a password to a non-auth command: " + verb);
		}

		if (!isValidPassword(password)) {
			throw new IllegalArgumentException("password cannot be expressed as a chat command");
		}

		StringBuilder line = new StringBuilder("/").append(verb.toLowerCase(Locale.ROOT));

		// Only registration ever repeats the password. A custom rule cannot talk the mod into
		// sending it more times than the command shape has room for.
		int allowed = registrationVerb(verb) ? 2 : 1;
		int times = Math.min(Math.max(1, repeats), allowed);

		for (int i = 0; i < times; i++) {
			line.append(' ').append(password);
		}

		return line.toString();
	}

	/** Register-side verbs: the only ones whose password is worth remembering. */
	public static boolean isRegisterVerb(String verb) {
		return verb != null && registrationVerb(verb);
	}

	private static boolean registrationVerb(String verb) {
		String lower = verb.toLowerCase(Locale.ROOT);
		return lower.startsWith("reg") || lower.startsWith("sign");
	}

	/** A version safe to put in the log: the verb, never the argument. */
	public static String describeCommand(String commandLine) {
		if (commandLine == null || commandLine.isEmpty()) {
			return "";
		}

		int space = commandLine.indexOf(' ');
		return space < 0 ? commandLine : commandLine.substring(0, space) + " <hidden>";
	}
}
