package com.monhenjo.autologin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pattern-based detection over {@link PromptRules}.
 *
 * <p>Ordering is deliberate. "You are now logged in" also contains the word login, and
 * "Successfully logged in" arrives right after we send a password, so the authenticated rules
 * are always consulted first and a match there suppresses every credential rule. Without that,
 * the mod would ask the user to pick a password for a session that is already open.
 */
public final class RuleBasedDetector implements LoginDetector {
	private final List<PromptRule> authenticated;
	private final List<PromptRule> credential;

	public RuleBasedDetector() {
		this(List.of());
	}

	/**
	 * @param customRules rules from the user's config, consulted before the shipped ones so a
	 *                    server with a custom message can be supported without a mod update
	 */
	public RuleBasedDetector(List<PromptRule> customRules) {
		List<PromptRule> authRules = new ArrayList<>();
		List<PromptRule> credentialRules = new ArrayList<>();

		for (PromptRule rule : customRules) {
			if (rule.kind() == LoginKind.AUTHENTICATED) {
				authRules.add(rule);
			} else {
				credentialRules.add(rule);
			}
		}

		authRules.addAll(PromptRules.builtInAuthenticated());
		credentialRules.addAll(PromptRules.builtIn());

		this.authenticated = List.copyOf(authRules);
		this.credential = List.copyOf(credentialRules);
	}

	@Override
	public Optional<LoginPrompt> detect(String plainText) {
		String text = normalize(plainText);
		if (text.isEmpty()) {
			return Optional.empty();
		}

		for (PromptRule rule : authenticated) {
			if (rule.matches(text)) {
				return Optional.of(prompt(rule, text));
			}
		}

		for (PromptRule rule : credential) {
			if (rule.matches(text)) {
				return Optional.of(prompt(rule, text));
			}
		}

		return Optional.empty();
	}

	private static LoginPrompt prompt(PromptRule rule, String text) {
		String verb = LoginVerbs.resolve(text, rule.kind(), rule.verb());
		return new LoginPrompt(rule.kind(), verb, rule.id(), text);
	}

	/** Collapses runs of whitespace so rules do not have to care about server formatting. */
	static String normalize(String text) {
		if (text == null) {
			return "";
		}

		StringBuilder out = new StringBuilder(text.length());
		boolean lastWasSpace = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c) || c == '\u00A0' || c == '\u2007' || c == '\u202F') {
				if (!lastWasSpace && out.length() > 0) {
					out.append(' ');
					lastWasSpace = true;
				}
			} else if (c != '\u00A7') {
				out.append(c);
				lastWasSpace = false;
			} else if (i + 1 < text.length()) {
				// Skip a literal legacy formatting code pair such as §c.
				i++;
			}
		}

		return out.toString().trim();
	}
}
