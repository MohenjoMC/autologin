package com.monhenjo.autologin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The shipped knowledge about what authentication plugins say, plus the parser for the
 * user's own rules.
 *
 * <p>Patterns are deliberately about the <em>prompt</em>, not about the word "login" - a
 * server that says "vote for us to unlock /login rewards" must not make the picker pop up.
 * Everything is matched against plain text, so a server that recolours or reformats its
 * message still matches.
 */
public final class PromptRules {
	private PromptRules() {
	}

	/** Rules that fire on a message the server would only send when asking for credentials. */
	public static List<PromptRule> builtIn() {
		List<PromptRule> rules = new ArrayList<>();

		// Register before login: "/register <password> <password>" also contains "/register".
		rules.add(PromptRule.of("register.usage", LoginKind.REGISTER,
			"/register\\s+(?:<[^>]*>|\\[[^\\]]*\\]|password)", "register"));
		rules.add(PromptRule.of("register.please", LoginKind.REGISTER,
			"\\bplease\\s+(?:register|sign\\s?up)\\b", "register"));
		rules.add(PromptRule.of("register.type", LoginKind.REGISTER,
			"\\btype\\s+/register\\b", "register"));
		rules.add(PromptRule.of("register.not-registered", LoginKind.REGISTER,
			"\\b(?:not|isn't)\\s+registered\\b", "register"));

		rules.add(PromptRule.of("login.usage", LoginKind.LOGIN,
			"/login\\s+(?:<[^>]*>|\\[[^\\]]*\\]|password|pass)", "login"));
		rules.add(PromptRule.of("login.short-usage", LoginKind.LOGIN,
			"/l\\s+(?:<[^>]*>|\\[[^\\]]*\\]|password)", "l"));
		rules.add(PromptRule.of("login.please", LoginKind.LOGIN,
			"\\bplease,?\\s+(?:log\\s?in|login|authenticate)\\b", "login"));
		rules.add(PromptRule.of("login.not-logged-in", LoginKind.LOGIN,
			"\\b(?:you\\s+are|you're)\\s+not\\s+(?:logged|authenticated)\\b", "login"));
		rules.add(PromptRule.of("login.type", LoginKind.LOGIN,
			"\\btype\\s+/(?:login|log|l|auth)\\b", "login"));
		rules.add(PromptRule.of("login.enter-password", LoginKind.LOGIN,
			"\\b(?:enter|input|provide)\\s+(?:your\\s+)?password\\b", "login"));
		rules.add(PromptRule.of("login.required", LoginKind.LOGIN,
			"\\blogin\\s+(?:is\\s+)?(?:required|needed)\\b", "login"));
		rules.add(PromptRule.of("login.starts-with-command", LoginKind.LOGIN,
			"^\\s*(?:\\[[^\\]]*\\]\\s*)*/(?:login|log|l|auth)\\b", "login"));

		return List.copyOf(rules);
	}

	/** Rules that mean the server no longer needs anything from us. */
	public static List<PromptRule> builtInAuthenticated() {
		List<PromptRule> rules = new ArrayList<>();
		rules.add(PromptRule.of("auth.success", LoginKind.AUTHENTICATED,
			"\\b(?:successfully|now)\\s+(?:logged|authenticated)\\b", "login"));
		rules.add(PromptRule.of("auth.success-suffix", LoginKind.AUTHENTICATED,
			"\\blogged\\s+in\\s+successfully\\b", "login"));
		rules.add(PromptRule.of("auth.already", LoginKind.AUTHENTICATED,
			"\\balready\\s+(?:logged\\s+in|authenticated)\\b", "login"));
		rules.add(PromptRule.of("auth.session", LoginKind.AUTHENTICATED,
			"\\bsession\\s+(?:is\\s+)?(?:still\\s+)?(?:valid|restored|verified|active)\\b", "login"));
		rules.add(PromptRule.of("auth.successful", LoginKind.AUTHENTICATED,
			"\\bauthentication\\s+successful\\b", "login"));
		rules.add(PromptRule.of("auth.welcome-back", LoginKind.AUTHENTICATED,
			"\\bwelcome\\s+back\\b", "login"));
		return List.copyOf(rules);
	}

	/**
	 * Parses the config's custom rules, one per line:
	 *
	 * <pre>
	 * LOGIN ^Please /login &lt;password&gt;$
	 * REGISTER /register\s+&lt;password&gt;
	 * AUTHENTICATED ^Access granted
	 * </pre>
	 *
	 * Bad lines are skipped rather than throwing, so one typo cannot lock the user out of
	 * every other rule. The caller gets the rejects back for the log/UI.
	 */
	public static ParseResult parseCustom(List<String> lines) {
		List<PromptRule> rules = new ArrayList<>();
		List<String> rejected = new ArrayList<>();

		if (lines == null) {
			return new ParseResult(rules, rejected);
		}

		int index = 0;
		for (String raw : lines) {
			index++;
			if (raw == null) {
				continue;
			}

			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			String[] parts = line.split("\\s+", 2);
			LoginKind kind = kindOf(parts[0]);
			if (kind == null || parts.length < 2 || parts[1].isBlank()) {
				rejected.add("line " + index + ": " + raw);
				continue;
			}

			try {
				rules.add(PromptRule.of("custom." + index, kind, parts[1]));
			} catch (RuntimeException e) {
				rejected.add("line " + index + ": " + raw + " (" + e.getMessage() + ")");
			}
		}

		return new ParseResult(rules, rejected);
	}

	private static LoginKind kindOf(String token) {
		try {
			return LoginKind.valueOf(token.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public record ParseResult(List<PromptRule> rules, List<String> rejected) {
	}
}
