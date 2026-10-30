package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real messages, as the plugins actually print them. The point of the suite is the pair of
 * failures that matter: missing a prompt (user types the password by hand) and firing on a
 * message that is not a prompt (picker opens during normal chat).
 */
class RuleBasedDetectorTest {
	private final LoginDetector detector = new RuleBasedDetector();

	private LoginPrompt detect(String text) {
		Optional<LoginPrompt> prompt = detector.detect(text);
		assertTrue(prompt.isPresent(), "expected a prompt for: " + text);
		return prompt.get();
	}

	private void assertNoPrompt(Optional<LoginPrompt> prompt) {
		assertFalse(prompt.isPresent(), () -> "unexpected prompt: " + prompt.orElse(null));
	}


	@Test
	void authMeLoginPrompt() {
		LoginPrompt prompt = detect("Please login with the command: /login <password>");
		assertEquals(LoginKind.LOGIN, prompt.kind());
		assertEquals("login", prompt.verb());
	}

	@Test
	void authMeRegisterPromptIsRegisterNotLogin() {
		LoginPrompt prompt = detect("Please register with the command: /register <password> <password>");
		assertEquals(LoginKind.REGISTER, prompt.kind());
		assertEquals("register", prompt.verb());
		assertEquals(2, prompt.passwordArguments(), "AuthMe wants the password twice");
	}

	@Test
	void registerWithOnePlaceholderWantsItOnce() {
		assertEquals(1, detect("Please register: /register [password]").passwordArguments());
	}

	@Test
	void loginAlwaysTakesOnePassword() {
		assertEquals(1, detect("Please login with /login <password>").passwordArguments());
	}

	@Test
	void registrationCountsAsAuthenticated() {
		assertEquals(LoginKind.AUTHENTICATED, detect("Successfully registered!").kind());
	}

	@Test
	void nloginPrompt() {
		assertEquals(LoginKind.LOGIN, detect("Please, login with /login <password>").kind());
	}

	@Test
	void libreLoginPrompt() {
		assertEquals(LoginKind.LOGIN, detect("You are not logged in. Please login with /login <password>.").kind());
	}

	@Test
	void loginSecurityPrompt() {
		assertEquals(LoginKind.LOGIN, detect("Please login using /login <password>").kind());
	}

	@Test
	void promptWithRankPrefix() {
		assertEquals(LoginKind.LOGIN, detect("[VIP] Please login with /login <password>").kind());
	}

	@Test
	void commandFirstFormatting() {
		assertEquals(LoginKind.LOGIN, detect("/login <password>").kind());
	}

	@Test
	void shortVerbIsTakenFromTheMessage() {
		assertEquals("l", detect("Please login: /l <password>").verb());
	}

	@Test
	void authenticatedWinsOverTheWordLogin() {
		assertEquals(LoginKind.AUTHENTICATED, detect("Successfully logged in!").kind());
		assertEquals(LoginKind.AUTHENTICATED, detect("You are now logged in").kind());
		assertEquals(LoginKind.AUTHENTICATED, detect("Your session is still valid.").kind());
	}

	@Test
	void serverAnnouncementsAreNotPrompts() {
		assertNoPrompt(detector.detect("Vote for us and win a /login reward!"));
		assertNoPrompt(detector.detect("Welcome to the server, have fun!"));
		assertNoPrompt(detector.detect("Steve joined the game"));
		assertNoPrompt(detector.detect("Use /login rewards to open the crate"));
		assertNoPrompt(detector.detect(""));
		assertNoPrompt(detector.detect(null));
	}

	@Test
	void waitMessageAfterSendingIsNotAPrompt() {
		assertNoPrompt(detector.detect("Please wait a moment before logging in again!"));
	}

	@Test
	void customRuleIsConsultedBeforeTheShippedOnes() {
		LoginDetector custom = new RuleBasedDetector(List.of(
			PromptRule.of("custom.gate", LoginKind.LOGIN, "^Gate closed, /auth \\S+$", "auth")));

		LoginPrompt prompt = custom.detect("Gate closed, /auth hunter2").orElseThrow();
		assertEquals("custom.gate", prompt.ruleId());
		assertEquals("auth", prompt.verb());
	}

	@Test
	void customAuthenticatedRuleStopsPrompts() {
		LoginDetector custom = new RuleBasedDetector(List.of(
			PromptRule.of("custom.done", LoginKind.AUTHENTICATED, "^Access granted")));

		assertEquals(LoginKind.AUTHENTICATED, custom.detect("Access granted").orElseThrow().kind());
	}

	@Test
	void whitespaceAndColourCodesAreNormalised() {
		assertEquals(LoginKind.LOGIN, detect("Please    login   with   /login <password>").kind());
		assertEquals(LoginKind.LOGIN, detect("§cPlease login with /login <password>").kind());
	}

	@Test
	void chatBroadcastWithASenderPrefix() {
		// Captured from a real integrated server: /say arrives as player chat, decorated with the
		// sender's display name.
		assertEquals(LoginKind.LOGIN, detect("[Server] Please login with the command: /login <password>").kind());
	}
}
