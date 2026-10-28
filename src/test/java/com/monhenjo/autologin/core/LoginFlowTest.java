package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginFlowTest {
	private static final String SERVER = "mc.example.com";

	private final LoginFlow flow = new LoginFlow();
	private final Account main = Account.create("main", "hunter2", SERVER);
	private final Account alt = Account.create("alt", "second", SERVER);

	private static LoginPrompt loginPrompt() {
		return new LoginPrompt(LoginKind.LOGIN, "login", "login.please", "Please login with /login <password>");
	}

	@Test
	void firstPromptAsksTheUser() {
		var decision = flow.onPrompt(loginPrompt(), List.of(main, alt), 1_000L).orElseThrow();
		assertEquals(2, decision.options().size());
		assertFalse(decision.retry());
	}

	@Test
	void burstOfIdenticalPromptsIsShownOnce() {
		assertTrue(flow.onPrompt(loginPrompt(), List.of(main), 1_000L).isPresent());
		assertTrue(flow.onPrompt(loginPrompt(), List.of(main), 1_500L).isEmpty());
		assertTrue(flow.onPrompt(loginPrompt(), List.of(main), 5_000L).isPresent());
	}

	@Test
	void secondAttemptIsMarkedAsARetry() {
		flow.onPrompt(loginPrompt(), List.of(main), 1_000L);
		flow.onPasswordSent(2_000L);

		var second = flow.onPrompt(loginPrompt(), List.of(main), 30_000L).orElseThrow();
		assertTrue(second.retry());
	}

	@Test
	void attemptsDecayAfterAMinute() {
		flow.onPrompt(loginPrompt(), List.of(main), 1_000L);
		flow.onPasswordSent(2_000L);

		var later = flow.onPrompt(loginPrompt(), List.of(main), 120_000L).orElseThrow();
		assertFalse(later.retry());
	}

	@Test
	void authenticatedSessionsStopPrompting() {
		flow.onAuthenticated();
		assertTrue(flow.onPrompt(loginPrompt(), List.of(main), 10_000L).isEmpty());
	}

	@Test
	void disconnectForgetsEverything() {
		flow.onPrompt(loginPrompt(), List.of(main), 1_000L);
		flow.onPasswordSent(2_000L);
		flow.onDisconnected();

		var fresh = flow.onPrompt(loginPrompt(), List.of(main), 3_000L).orElseThrow();
		assertFalse(fresh.retry());
		assertFalse(flow.authenticated());
	}

	@Test
	void commandUsesTheVerbTheServerAskedFor() {
		var prompt = new LoginPrompt(LoginKind.REGISTER, "register", "register.please", "Please register");
		flow.onPrompt(prompt, List.of(main), 1_000L);

		assertEquals("/register hunter2", flow.commandFor(main).orElseThrow());
	}

	@Test
	void commandFallsBackToLoginWithoutAPrompt() {
		assertEquals("/login hunter2", new LoginFlow().commandFor(main).orElseThrow());
	}

	@Test
	void unusablePasswordProducesNoCommand() {
		Account broken = Account.create("broken", "has space", SERVER);
		assertTrue(new LoginFlow().commandFor(broken).isEmpty());
		assertFalse(broken.usable());
	}

	@Test
	void noAccountsStillProducesADecision() {
		var decision = flow.onPrompt(loginPrompt(), List.of(), 1_000L).orElseThrow();
		assertTrue(decision.options().isEmpty());
	}

	@Test
	void dismissingAsksAgainButDoesNotFakeAAuthentication() {
		flow.onPrompt(loginPrompt(), List.of(main), 1_000L);
		flow.onDismissed();

		assertFalse(flow.awaitingCredentials());
		assertTrue(flow.onPrompt(loginPrompt(), List.of(main), 2_000L).isPresent());
		assertFalse(flow.authenticated());
	}
}
