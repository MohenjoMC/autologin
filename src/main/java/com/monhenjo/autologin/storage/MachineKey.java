package com.monhenjo.autologin.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * A machine fingerprint used to derive the vault key.
 *
 * <p>This is not a secret and is not treated as one. It ties the encrypted file to "this user on
 * this machine", which is the actual threat being answered: a config folder synced to a cloud
 * drive, a backup, or a screenshot of a file listing. Someone who has the client and the file
 * can always decrypt, and the README says so.
 *
 * <p>Reads are cached: /etc/machine-id does not change while the game is running, and the
 * fingerprint is asked for on every save.
 */
public final class MachineKey {
	private static volatile String cached;

	private MachineKey() {
	}

	public static String fingerprint() {
		String value = cached;
		if (value == null) {
			value = build();
			cached = value;
		}

		return value;
	}

	private static String build() {
		StringBuilder sb = new StringBuilder();

		sb.append("os=").append(System.getProperty("os.name", "?").toLowerCase(Locale.ROOT));
		sb.append(";arch=").append(System.getProperty("os.arch", "?"));
		sb.append(";user=").append(System.getProperty("user.name", "?"));
		sb.append(";machine=").append(machineId());

		return sb.toString();
	}

	private static String machineId() {
		for (String candidate : new String[] {
			"/etc/machine-id",
			"/var/lib/dbus/machine-id",
			"/sys/class/dmi/id/product_uuid"
		}) {
			try {
				String value = Files.readString(Path.of(candidate), StandardCharsets.UTF_8).trim();
				if (!value.isEmpty()) {
					return value;
				}
			} catch (IOException | RuntimeException ignored) {
				// Not present on this platform; fall through to the portable parts.
			}
		}

		String host = System.getenv("COMPUTERNAME");
		if (host == null) {
			host = System.getenv("HOSTNAME");
		}

		return host == null ? "unknown-host" : host;
	}
}
