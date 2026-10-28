package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginVerbsTest {
	@Test
	void buildsAnAllowlistedCommand() {
		assertEquals("/login hunter2", LoginVerbs.build("login", "hunter2"));
		assertEquals("/register hunter2", LoginVerbs.build("register", "hunter2"));
	}

	@Test
	void refusesVerbsThatAreNotAuthentication() {
		// A server can print anything; the mod must not be talked into /msg or /pay.
		assertFalse(LoginVerbs.isAllowed("msg"));
		assertFalse(LoginVerbs.isAllowed("pay"));
		assertThrows(IllegalArgumentException.class, () -> LoginVerbs.build("msg", "hunter2"));
		assertThrows(IllegalArgumentException.class, () -> LoginVerbs.build(null, "hunter2"));
	}

	@Test
	void refusesPasswordsThatCannotSurviveAChatCommand() {
		assertThrows(IllegalArgumentException.class, () -> LoginVerbs.build("login", "two words"));
		assertThrows(IllegalArgumentException.class, () -> LoginVerbs.build("login", "line\nbreak"));
		assertThrows(IllegalArgumentException.class, () -> LoginVerbs.build("login", ""));
		assertFalse(LoginVerbs.isValidPassword("tab\there"));
		assertFalse(LoginVerbs.isValidPassword(null));
	}

	@Test
	void takesTheVerbFromTheMessageWhenItIsAllowlisted() {
		assertEquals("l", LoginVerbs.resolve("/l <password>", LoginKind.LOGIN, "login"));
		assertEquals("login", LoginVerbs.resolve("/login <password>", LoginKind.LOGIN, null));
	}

	@Test
	void ignoresCommandsThatAreNotAuthentication() {
		// The first slash-command in the text is not an auth verb, so it must not be used.
		assertEquals("login", LoginVerbs.resolve("open /shop then /login <password>", LoginKind.LOGIN, null));
	}

	@Test
	void fallsBackToTheKindDefault() {
		assertEquals("login", LoginVerbs.resolve("no command here", LoginKind.LOGIN, null));
		assertEquals("register", LoginVerbs.resolve("no command here", LoginKind.REGISTER, null));
	}

	@Test
	void describesWithoutLeakingThePassword() {
		assertEquals("/login <hidden>", LoginVerbs.describeCommand("/login hunter2"));
		assertEquals("/login", LoginVerbs.describeCommand("/login"));
		assertTrue(LoginVerbs.describeCommand("/login hunter2").indexOf("hunter2") < 0);
	}
}
