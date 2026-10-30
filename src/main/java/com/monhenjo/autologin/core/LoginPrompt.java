package com.monhenjo.autologin.core;

import java.util.Objects;

/**
 * One detected request from a server to authenticate.
 *
 * @param kind              what the server is asking for
 * @param verb              the command verb to use, already filtered through {@link LoginVerbs#isAllowed(String)}
 * @param ruleId            id of the rule that matched, useful in the debug log
 * @param text              the plain text the server sent, for display and logging
 * @param passwordArguments how many times the password has to appear on the command line. AuthMe,
 *                          nLogin and LibreLogin all ask for {@code /register <password> <password>},
 *                          so this is read from the prompt's own usage line rather than assumed.
 */
public record LoginPrompt(LoginKind kind, String verb, String ruleId, String text, int passwordArguments) {
	public LoginPrompt {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(verb, "verb");
		Objects.requireNonNull(ruleId, "ruleId");
		text = text == null ? "" : text;
		passwordArguments = Math.max(1, passwordArguments);
	}

	/** Prompt with the plugin's usual arity: one for login, two for register. */
	public LoginPrompt(LoginKind kind, String verb, String ruleId, String text) {
		this(kind, verb, ruleId, text, kind == LoginKind.REGISTER ? 2 : 1);
	}

	/** True while the server is still waiting for credentials. */
	public boolean needsCredentials() {
		return kind == LoginKind.LOGIN || kind == LoginKind.REGISTER;
	}
}
