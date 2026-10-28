package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptRulesTest {
	@Test
	void parsesKindAndRegex() {
		var parsed = PromptRules.parseCustom(List.of(
			"LOGIN ^Please /login <password>$",
			"REGISTER /register\\s+<password>",
			"AUTHENTICATED ^Access granted"
		));

		assertEquals(3, parsed.rules().size());
		assertTrue(parsed.rejected().isEmpty());
		assertEquals(LoginKind.LOGIN, parsed.rules().get(0).kind());
		assertEquals(LoginKind.REGISTER, parsed.rules().get(1).kind());
		assertEquals(LoginKind.AUTHENTICATED, parsed.rules().get(2).kind());
	}

	@Test
	void ignoresCommentsAndBlankLines() {
		var parsed = PromptRules.parseCustom(List.of("", "   ", "# a note", "LOGIN /login"));
		assertEquals(1, parsed.rules().size());
		assertTrue(parsed.rejected().isEmpty());
	}

	@Test
	void rejectsBadLinesWithoutLosingTheRest() {
		var parsed = PromptRules.parseCustom(List.of(
			"WHATEVER /login",
			"LOGIN",
			"LOGIN [unclosed",
			"LOGIN /login fine"
		));

		assertEquals(1, parsed.rules().size());
		assertEquals(3, parsed.rejected().size());
	}

	@Test
	void handlesNull() {
		assertTrue(PromptRules.parseCustom(null).rules().isEmpty());
	}
}
