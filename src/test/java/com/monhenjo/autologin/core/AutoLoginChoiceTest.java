package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoLoginChoiceTest {
	private final Account main = Account.create("main", "one", "mc.example.com");
	private final Account alt = Account.create("alt", "two", "mc.example.com");

	@Test
	void preferredLabelWins() {
		assertEquals("alt", AutoLoginChoice.choose("alt", List.of(main, alt)).orElseThrow().label());
	}

	@Test
	void preferredLabelIsCaseInsensitive() {
		assertEquals("main", AutoLoginChoice.choose("MAIN", List.of(main, alt)).orElseThrow().label());
	}

	@Test
	void unknownPreferredFallsBackToTheFirstRow() {
		assertEquals("main", AutoLoginChoice.choose("nope", List.of(main, alt)).orElseThrow().label());
	}

	@Test
	void nothingSavedMeansNothingToDo() {
		assertTrue(AutoLoginChoice.choose("main", List.of()).isEmpty());
		assertTrue(AutoLoginChoice.choose(null, List.of()).isEmpty());
	}

	@Test
	void aTypedLabelResolvesToItsAccount() {
		assertEquals("main", AutoLoginChoice.matchLabel("main", List.of(main, alt)).orElseThrow().label());
	}

	@Test
	void aTypedPasswordIsNotALabel() {
		// Anything that is not a saved label has to be forwarded to the server untouched.
		assertTrue(AutoLoginChoice.matchLabel("hunter2", List.of(main, alt)).isEmpty());
		assertTrue(AutoLoginChoice.matchLabel("", List.of(main, alt)).isEmpty());
	}

	@Test
	void suggestionsListLabelsWithTheTypedPrefix() {
		assertEquals(List.of("main"), AutoLoginChoice.suggest("ma", List.of(main, alt)));
		assertEquals(List.of("main", "alt"), AutoLoginChoice.suggest("", List.of(main, alt)));
		assertTrue(AutoLoginChoice.suggest("zz", List.of(main, alt)).isEmpty());
	}
}
