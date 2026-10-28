package com.monhenjo.autologin.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultTest {
	private static final byte[] SALT = Vault.newSalt();

	@Test
	void roundTrips() throws Exception {
		Vault vault = Vault.withMaterial(SALT, "machine-a");
		byte[] plain = "hunter2".getBytes(StandardCharsets.UTF_8);

		byte[] sealed = vault.seal(plain);
		assertFalse(Arrays.equals(plain, sealed), "ciphertext must not equal the plaintext");
		assertArrayEquals(plain, vault.open(sealed));
	}

	@Test
	void sameSaltAndMaterialCanReadItBack() throws Exception {
		byte[] sealed = Vault.withMaterial(SALT, "machine-a").seal("x".getBytes(StandardCharsets.UTF_8));
		assertArrayEquals("x".getBytes(StandardCharsets.UTF_8), Vault.withMaterial(SALT, "machine-a").open(sealed));
	}

	@Test
	void anotherMachineCannotOpenIt() {
		byte[] sealed = Vault.withMaterial(SALT, "machine-a").seal("x".getBytes(StandardCharsets.UTF_8));
		Vault other = Vault.withMaterial(SALT, "machine-b");

		assertThrows(Vault.VaultException.class, () -> other.open(sealed));
	}

	@Test
	void tamperedCiphertextIsRejected() {
		Vault vault = Vault.withMaterial(SALT, "machine-a");
		byte[] sealed = vault.seal("hunter2".getBytes(StandardCharsets.UTF_8));
		sealed[sealed.length - 1] ^= 0x01;

		assertThrows(Vault.VaultException.class, () -> vault.open(sealed));
	}

	@Test
	void truncatedFileIsRejected() {
		Vault vault = Vault.withMaterial(SALT, "machine-a");
		assertThrows(Vault.VaultException.class, () -> vault.open(new byte[] {1, 2, 3}));
		assertThrows(Vault.VaultException.class, () -> vault.open(null));
	}

	@Test
	void saltIsRandomPerCall() {
		assertFalse(Arrays.equals(Vault.newSalt(), Vault.newSalt()));
		assertTrue(Vault.newSalt().length >= 16);
	}
}
