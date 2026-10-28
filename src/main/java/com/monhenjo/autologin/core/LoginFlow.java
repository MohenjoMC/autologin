package com.monhenjo.autologin.core;

import java.util.List;
import java.util.Optional;

/**
 * The session state machine between "the server asked for a password" and "the server stopped
 * asking".
 *
 * <p>Kept free of Minecraft on purpose: every rule here (dedupe bursts, count retries, forget
 * everything on disconnect) is testable with plain longs instead of ticks and packets. The
 * client only supplies two numbers - the current time and the account list - and acts on the
 * decision.
 *
 * <p>The mod never sends a password without a click. This class therefore has no "auto send"
 * decision at all; it can only report that the picker should be shown.
 */
public final class LoginFlow {
	/** Repeated copies of the same prompt inside this window are treated as one prompt. */
	private static final long PROMPT_DEDUPE_MILLIS = 2_000L;

	/** After this long without another prompt, the next one counts as a fresh attempt. */
	private static final long ATTEMPT_DECAY_MILLIS = 60_000L;

	private final long dedupeMillis;

	private LoginPrompt active;
	private boolean authenticated;
	private long lastPromptAt = Long.MIN_VALUE;
	private long lastSentAt = Long.MIN_VALUE;
	private int attempts;

	public LoginFlow() {
		this(PROMPT_DEDUPE_MILLIS);
	}

	public LoginFlow(long dedupeMillis) {
		this.dedupeMillis = Math.max(0L, dedupeMillis);
	}

	/**
	 * @return a decision when the user should be shown the picker, or empty when this message is
	 *         a duplicate, arrives after authentication, or is not a credential prompt at all
	 */
	public Optional<Decision> onPrompt(LoginPrompt prompt, List<Account> options, long now) {
		if (prompt == null || !prompt.needsCredentials() || authenticated) {
			return Optional.empty();
		}

		boolean duplicate = active != null
			&& prompt.text().equalsIgnoreCase(active.text())
			&& now - lastPromptAt < dedupeMillis;

		active = prompt;
		if (duplicate) {
			return Optional.empty();
		}

		lastPromptAt = now;
		if (now - lastSentAt > ATTEMPT_DECAY_MILLIS) {
			attempts = 0;
		}

		boolean retry = attempts > 0;
		return Optional.of(new Decision(prompt, List.copyOf(options), retry));
	}

	/** Records that the server announced a successful login, or that we observed one. */
	public void onAuthenticated() {
		authenticated = true;
		active = null;
		attempts = 0;
	}

	public void onPasswordSent(long now) {
		lastSentAt = now;
		attempts++;
	}

	/** The player closed the prompt without sending anything: forget the prompt, keep the session. */
	public void onDismissed() {
		active = null;
		lastPromptAt = Long.MIN_VALUE;
		attempts = 0;
	}

	/** A new server, or the same one after a reconnect: forget everything. */
	public void onDisconnected() {
		active = null;
		authenticated = false;
		lastPromptAt = Long.MIN_VALUE;
		lastSentAt = Long.MIN_VALUE;
		attempts = 0;
	}

	/**
	 * The command line for an account, or empty when it cannot be expressed safely (empty
	 * password, spaces, control characters) - the UI then explains instead of sending.
	 */
	public Optional<String> commandFor(Account account) {
		if (account == null) {
			return Optional.empty();
		}

		String verb = active == null ? null : active.verb();
		if (verb == null || !LoginVerbs.isAllowed(verb)) {
			verb = "login";
		}

		try {
			return Optional.of(LoginVerbs.build(verb, account.password()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public boolean authenticated() {
		return authenticated;
	}

	public boolean awaitingCredentials() {
		return active != null && !authenticated;
	}

	public int attempts() {
		return attempts;
	}

	public Optional<LoginPrompt> activePrompt() {
		return Optional.ofNullable(active);
	}

	public record Decision(LoginPrompt prompt, List<Account> options, boolean retry) {
	}
}
