package com.monhenjo.autologin.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One "this text means the server wants credentials" rule.
 *
 * @param id      stable id, shown in the debug log so a false positive can be traced
 * @param kind    what the rule recognises
 * @param pattern matched with {@code find()} against the plain text of a server message
 * @param verb    preferred command verb, or {@code null} to take it from the message
 */
public record PromptRule(String id, LoginKind kind, Pattern pattern, String verb) {
	public PromptRule {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(pattern, "pattern");
	}

	public static PromptRule of(String id, LoginKind kind, String regex) {
		return new PromptRule(id, kind, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), null);
	}

	public static PromptRule of(String id, LoginKind kind, String regex, String verb) {
		return new PromptRule(id, kind, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), verb);
	}

	public boolean matches(String text) {
		return pattern.matcher(text).find();
	}
}
