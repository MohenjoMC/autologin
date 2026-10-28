package com.monhenjo.autologin.core;

import java.util.Objects;
import java.util.UUID;

/**
 * One saved password, and the server it belongs to.
 *
 * <p>Plain {@link String} for the password on purpose: a mod that renders, copies and sends the
 * value cannot meaningfully keep it out of the heap, so pretending with {@code char[]} would only
 * add ceremony. What is defended is the file on disk (see {@code storage.Vault}) and the wire
 * (see {@link LoginVerbs}).
 */
public final class Account {
	private final String id;
	private final String label;
	private final String password;
	private final String serverKey;
	private final long createdAt;
	private long lastUsedAt;
	private int uses;

	public Account(String id, String label, String password, String serverKey, long createdAt, long lastUsedAt, int uses) {
		this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
		this.label = label == null ? "" : label.trim();
		this.password = password == null ? "" : password;
		this.serverKey = ServerKey.normalize(serverKey);
		this.createdAt = createdAt;
		this.lastUsedAt = lastUsedAt;
		this.uses = uses;
	}

	public static Account create(String label, String password, String serverKey) {
		long now = System.currentTimeMillis();
		return new Account(null, label, password, serverKey, now, 0L, 0);
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public String password() {
		return password;
	}

	public String serverKey() {
		return serverKey;
	}

	public long createdAt() {
		return createdAt;
	}

	public long lastUsedAt() {
		return lastUsedAt;
	}

	public int uses() {
		return uses;
	}

	public Account withLastUsed(long epochMillis, int newUses) {
		Account copy = new Account(id, label, password, serverKey, createdAt, epochMillis, newUses);
		return copy;
	}

	public boolean usable() {
		return LoginVerbs.isValidPassword(password);
	}

	/** A label for UI and logs: never the password itself. */
	public String displayName() {
		if (!label.isEmpty()) {
			return label;
		}

		return serverKey.isEmpty() || ServerKey.ANY.equals(serverKey) ? "unnamed" : serverKey;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Account account && id.equals(account.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Account[" + displayName() + " @ " + serverKey + "]";
	}
}
