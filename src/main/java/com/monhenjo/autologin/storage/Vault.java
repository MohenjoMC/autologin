package com.monhenjo.autologin.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM sealing for the accounts file.
 *
 * <p>Key = PBKDF2-HMAC-SHA256 of the {@link MachineKey} fingerprint and a random salt kept next
 * to the vault. Losing the salt file means losing the passwords (they can be re-entered); copying
 * the salt file and the vault to another machine is not enough to read them.
 *
 * <p>Sealed layout is {@code iv || ciphertext || tag}, all in one blob, so the store only has to
 * Base64 it into JSON.
 */
public final class Vault {
	private static final String CIPHER = "AES/GCM/NoPadding";
	private static final String KDF = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 210_000;
	private static final int KEY_BITS = 256;
	private static final int SALT_BYTES = 32;
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final SecretKey key;

	private Vault(SecretKey key) {
		this.key = key;
	}

	/** Opens (or creates) the salt file and derives the key for this machine. */
	public static Vault forLocalMachine(Path saltFile) throws IOException {
		byte[] salt = readOrCreateSalt(saltFile);
		return new Vault(derive(salt, MachineKey.fingerprint()));
	}

	/** Explicit material, for tests and for "the salt file and the machine moved together". */
	public static Vault withMaterial(byte[] salt, String material) {
		return new Vault(derive(salt, material));
	}

	public static byte[] newSalt() {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		return salt;
	}

	public byte[] seal(byte[] plain) {
		try {
			byte[] iv = new byte[IV_BYTES];
			RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] body = cipher.doFinal(plain);

			byte[] out = new byte[iv.length + body.length];
			System.arraycopy(iv, 0, out, 0, iv.length);
			System.arraycopy(body, 0, out, iv.length, body.length);
			return out;
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES-GCM is unavailable in this JVM", e);
		}
	}

	public byte[] open(byte[] sealed) throws VaultException {
		if (sealed == null || sealed.length <= IV_BYTES) {
			throw new VaultException("vault file is empty or truncated");
		}

		try {
			byte[] iv = Arrays.copyOfRange(sealed, 0, IV_BYTES);
			byte[] body = Arrays.copyOfRange(sealed, IV_BYTES, sealed.length);

			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return cipher.doFinal(body);
		} catch (GeneralSecurityException e) {
			throw new VaultException("vault cannot be decrypted on this machine", e);
		}
	}

	private static SecretKey derive(byte[] salt, String material) {
		PBEKeySpec spec = new PBEKeySpec(material.toCharArray(), salt, ITERATIONS, KEY_BITS);

		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
			return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("PBKDF2 is unavailable in this JVM", e);
		} finally {
			spec.clearPassword();
		}
	}

	private static byte[] readOrCreateSalt(Path saltFile) throws IOException {
		if (Files.exists(saltFile)) {
			byte[] existing = Files.readAllBytes(saltFile);
			if (existing.length >= 16) {
				return existing;
			}

			throw new IOException("salt file at " + saltFile + " is truncated");
		}

		Path parent = saltFile.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		byte[] salt = newSalt();
		Files.write(saltFile, salt, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		restrictToOwner(saltFile);
		return salt;
	}

	/** Best effort: POSIX only, and silently skipped on Windows. */
	static void restrictToOwner(Path file) {
		try {
			Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
			Files.setPosixFilePermissions(file, ownerOnly);
		} catch (IOException | UnsupportedOperationException ignored) {
			// Windows and some network filesystems have no POSIX modes.
		}
	}

	/** Thrown when the bytes cannot be opened with the current key. */
	public static final class VaultException extends Exception {
		public VaultException(String message) {
			super(message);
		}

		public VaultException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
