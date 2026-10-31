package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountLineTest {
	@Test
	void roundTripsALabelServerAndPassword() {
		Optional<AccountLine> parsed = AccountLine.parse("main|mc.example.com|hunter2");
		assertTrue(parsed.isPresent());
		assertEquals("main", parsed.get().label());
		assertEquals("mc.example.com", parsed.get().serverKey());
		assertEquals("hunter2", parsed.get().password());
		assertEquals("main|mc.example.com|hunter2", parsed.get().toString());
	}

	@Test
	void aPipeInsideThePasswordIsKept() {
		// Only the first two pipes separate fields; a password may contain the rest.
		Optional<AccountLine> parsed = AccountLine.parse("alt|*|pa|ss");
		assertTrue(parsed.isPresent());
		assertEquals("pa|ss", parsed.get().password());
	}

	@Test
	void emptyServerMeansEveryServer() {
		assertEquals(ServerKey.ANY, AccountLine.parse("main||hunter2").orElseThrow().serverKey());
		assertEquals(ServerKey.ANY, AccountLine.parse("main|*|hunter2").orElseThrow().serverKey());
	}

	@Test
	void junkIsNotAProfile() {
		assertTrue(AccountLine.parse("main|hunter2").isEmpty(), "two fields is not enough");
		assertTrue(AccountLine.parse("|host|hunter2").isEmpty(), "no label");
		assertTrue(AccountLine.parse("main|host|").isEmpty(), "no password");
		assertTrue(AccountLine.parse("main|host|two words").isEmpty(), "password cannot be sent");
		assertTrue(AccountLine.parse("").isEmpty());
		assertTrue(AccountLine.parse(null).isEmpty());
	}

	@Test
	void convertsToAnAccountThatCanBeSent() {
		Account account = AccountLine.parse("main|localhost|hunter2").orElseThrow().toAccount();
		assertEquals("main", account.label());
		assertEquals("main", account.id());
		assertTrue(account.usable());
		assertFalse(account.password().isEmpty());
	}
}
