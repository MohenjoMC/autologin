package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterCaptureTest {
	private final RegisterCapture capture = new RegisterCapture(30_000L);

	@Test
	void remembersAMatchingRegistration() {
		assertTrue(capture.onCommand("mc.example.com", "/register hunter2 hunter2", 1_000L));
		assertEquals("hunter2", capture.consumeIfRecent(2_000L).orElseThrow().password());
	}

	@Test
	void acceptsTheAliasAndASingleArgument() {
		assertTrue(capture.onCommand("mc.example.com", "/reg hunter2", 1_000L));
		assertTrue(capture.onCommand("mc.example.com", "register hunter2 hunter2", 1_000L));
	}

	@Test
	void aMismatchedConfirmationIsATypoAndIsNotRemembered() {
		assertFalse(capture.onCommand("mc.example.com", "/register hunter2 hunter3", 1_000L));
		assertTrue(capture.consumeIfRecent(2_000L).isEmpty());
	}

	@Test
	void loginsAreNotRegistrations() {
		assertFalse(capture.onCommand("mc.example.com", "/login hunter2", 1_000L));
		assertFalse(capture.onCommand("mc.example.com", "/l hunter2", 1_000L));
		assertFalse(capture.onCommand("mc.example.com", "/msg Bob hunter2", 1_000L));
		assertTrue(capture.consumeIfRecent(2_000L).isEmpty());
	}

	@Test
	void aPasswordWithSpacesIsNotACommandPassword() {
		assertFalse(RegisterCapture.passwordOf("/register two words two words").isPresent());
	}

	@Test
	void aStaleRegistrationIsDropped() {
		capture.onCommand("mc.example.com", "/register hunter2 hunter2", 1_000L);
		assertTrue(capture.consumeIfRecent(60_000L).isEmpty());
		assertTrue(capture.pending().isEmpty(), "consuming clears it either way");
	}

	@Test
	void savingTwiceIsImpossible() {
		capture.onCommand("mc.example.com", "/register hunter2 hunter2", 1_000L);
		assertTrue(capture.consumeIfRecent(2_000L).isPresent());
		assertTrue(capture.consumeIfRecent(2_100L).isEmpty(), "a repeated success line must not save again");
	}

	@Test
	void labelsComeFromThePlayerAndStayUnique() {
		Account existing = Account.create("CoderSoft", "x", "mc.example.com");
		assertEquals("CoderSoft2", RegisterCapture.labelFor("CoderSoft", List.of(existing)));
		assertEquals("Someone", RegisterCapture.labelFor("Someone", List.of(existing)));
		assertEquals("main", RegisterCapture.labelFor("", List.of()));
		assertEquals("main", RegisterCapture.labelFor(null, null));
	}
}
