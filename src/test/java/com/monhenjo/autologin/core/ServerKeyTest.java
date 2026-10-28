package com.monhenjo.autologin.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerKeyTest {
	@Test
	void stripsTheDefaultPort() {
		assertEquals("mc.example.com", ServerKey.normalize("mc.example.com:25565"));
		assertEquals("mc.example.com", ServerKey.normalize("MC.Example.com"));
	}

	@Test
	void keepsANonDefaultPort() {
		assertEquals("mc.example.com:25566", ServerKey.normalize("mc.example.com:25566"));
	}

	@Test
	void stripsSchemeAndPath() {
		assertEquals("mc.example.com", ServerKey.normalize("minecraft://mc.example.com/"));
		assertEquals("mc.example.com", ServerKey.normalize("  mc.example.com  "));
	}

	@Test
	void handlesIpv6Literals() {
		assertEquals("[::1]", ServerKey.normalize("[::1]:25565"));
		assertEquals("[::1]:25566", ServerKey.normalize("[::1]:25566"));
	}

	@Test
	void matchesTheSameAddressWrittenDifferently() {
		assertTrue(ServerKey.matches("mc.example.com:25565", "mc.example.com"));
		assertTrue(ServerKey.matches("", "mc.example.com"));
		assertTrue(ServerKey.matches(ServerKey.ANY, "anything:25565"));
		assertFalse(ServerKey.matches("mc.example.com", "mc.example.com:25566"));
		assertFalse(ServerKey.matches("other.example.com", "mc.example.com"));
	}

	@Test
	void displayNeverSaysNothing() {
		assertEquals("this server", ServerKey.display(""));
		assertEquals("this server", ServerKey.display(null));
	}
}
