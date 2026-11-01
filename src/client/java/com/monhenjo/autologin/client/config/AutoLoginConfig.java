package com.monhenjo.autologin.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything, in one Cloth Config screen with three tabs: behaviour, saved logins, detection rules.
 *
 * <p>Each login is an object, not a packed string, so Cloth draws it as a small form with a field
 * per value. A {@code label|server|password} row was the storage format showing through, and it
 * read like a bug. The cost of keeping the passwords here is that the config file holds them in
 * plain text; it is written owner-only and the tab's tooltip says so.
 */
@Config(name = "autologin")
public class AutoLoginConfig implements ConfigData {
	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean enabled = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean autoLogin = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public String preferredLogin = "";

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean chatList = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean detectInSingleplayer = false;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean debug = false;

	@ConfigEntry.Category("logins")
	@ConfigEntry.Gui.Tooltip
	public List<LoginEntry> logins = new ArrayList<>();

	@ConfigEntry.Category("rules")
	@ConfigEntry.Gui.Tooltip
	public List<String> customRules = new ArrayList<>();

	/**
	 * Rows from builds that packed a login into {@code label|server|password}. Kept so old config
	 * files still load; emptied by {@link #validatePostLoad()} once migrated into {@link #logins}.
	 */
	@ConfigEntry.Category("logins")
	@ConfigEntry.Gui.Excluded
	public List<String> accounts = new ArrayList<>();

	@Override
	public void validatePostLoad() throws ValidationException {
		if (logins == null) {
			logins = new ArrayList<>();
		}

		if (accounts == null) {
			accounts = new ArrayList<>();
		}

		if (customRules == null) {
			customRules = new ArrayList<>();
		}

		if (preferredLogin == null) {
			preferredLogin = "";
		}

		migratePackedRows();

		logins.removeIf(entry -> entry == null || entry.label == null || entry.label.isBlank());
		customRules.removeIf(rule -> rule == null || rule.isBlank());
	}

	private void migratePackedRows() {
		if (!logins.isEmpty()) {
			accounts.clear();
			return;
		}

		for (String row : accounts) {
			com.monhenjo.autologin.core.AccountLine.parse(row).ifPresent(parsed -> {
				LoginEntry entry = new LoginEntry();
				entry.label = parsed.label();
				entry.server = parsed.serverKey();
				entry.password = parsed.password();
				logins.add(entry);
			});
		}

		accounts.clear();
	}

	/** One saved login as Cloth draws it: three labelled fields. */
	public static class LoginEntry {
		public String label = "";
		public String server = "";
		public String password = "";

		public LoginEntry() {
		}

		public LoginEntry(String label, String server, String password) {
			this.label = label == null ? "" : label;
			this.server = server == null ? "" : server;
			this.password = password == null ? "" : password;
		}
	}
}
