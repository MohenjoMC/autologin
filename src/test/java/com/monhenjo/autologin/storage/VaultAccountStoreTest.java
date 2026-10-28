package com.monhenjo.autologin.storage;

import com.monhenjo.autologin.core.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultAccountStoreTest {
	@TempDir
	Path dir;

	@Test
	void savesAndReloads() {
		VaultAccountStore store = VaultAccountStore.open(dir);
		store.add("main", "hunter2", "mc.example.com");

		VaultAccountStore reopened = VaultAccountStore.open(dir);
		List<Account> accounts = reopened.all();

		assertEquals(1, accounts.size());
		assertEquals("main", accounts.get(0).label());
		assertEquals("hunter2", accounts.get(0).password());
		assertTrue(reopened.problem().isEmpty());
	}

	@Test
	void passwordIsNotInTheFileInPlainText() throws Exception {
		VaultAccountStore store = VaultAccountStore.open(dir);
		store.add("main", "hunter2", "mc.example.com");

		String onDisk = Files.readString(dir.resolve("accounts.vault"), StandardCharsets.UTF_8);
		assertFalse(onDisk.contains("hunter2"));
		assertFalse(onDisk.contains("main"), "the label is inside the sealed payload too");
	}

	@Test
	void fileIsOwnerOnly() throws Exception {
		VaultAccountStore store = VaultAccountStore.open(dir);
		store.add("main", "hunter2", "mc.example.com");

		Path file = dir.resolve("accounts.vault");
		try {
			var permissions = Files.getPosixFilePermissions(file);
			assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(permissions));
		} catch (UnsupportedOperationException e) {
			// Not a POSIX filesystem; nothing to assert.
		}
	}

	@Test
	void filtersByServer() {
		VaultAccountStore store = VaultAccountStore.open(dir);
		store.add("main", "one", "mc.example.com:25565");
		store.add("other", "two", "other.example.com");
		store.add("any", "three", "*");

		List<Account> offered = store.forServer("mc.example.com");
		assertEquals(2, offered.size());
		assertTrue(offered.stream().anyMatch(account -> account.label().equals("any")));
	}

	@Test
	void unreadableVaultIsReportedAndNeverOverwritten() throws Exception {
		Path file = dir.resolve("accounts.vault");
		Files.writeString(file, "this is not a vault", StandardCharsets.UTF_8);

		VaultAccountStore store = VaultAccountStore.open(dir);
		assertTrue(store.problem().isPresent());
		assertTrue(store.all().isEmpty());

		store.add("main", "hunter2", "mc.example.com");
		assertEquals("this is not a vault", Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	void vaultCopiedWithoutItsSaltCannotBeRead() throws Exception {
		VaultAccountStore store = VaultAccountStore.open(dir);
		store.add("main", "hunter2", "mc.example.com");

		// Simulate a copy of the vault to another install: same file, new salt.
		Path other = Files.createDirectory(dir.resolve("other"));
		Files.copy(dir.resolve("accounts.vault"), other.resolve("accounts.vault"));
		Files.write(other.resolve("vault.salt"), Vault.newSalt());

		VaultAccountStore copied = VaultAccountStore.open(other);
		assertTrue(copied.problem().isPresent());
		assertTrue(copied.all().isEmpty());
	}

	@Test
	void markingUsedReordersThePicker() {
		VaultAccountStore store = VaultAccountStore.open(dir);
		Account first = store.add("first", "one", "mc.example.com");
		Account second = store.add("second", "two", "mc.example.com");

		store.markUsed(first.id(), System.currentTimeMillis() + 1_000L);

		assertEquals("first", store.all().get(0).label());
		assertEquals(0, store.find(second.id()).orElseThrow().uses());
		assertEquals(1, store.find(first.id()).orElseThrow().uses());
		assertNotEquals(first.id(), second.id());
	}

	@Test
	void rejectsPasswordsThatCannotBeSent() {
		VaultAccountStore store = VaultAccountStore.open(dir);
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> store.add("bad", "two words", "mc.example.com"));
		assertTrue(store.all().isEmpty());
	}
}
