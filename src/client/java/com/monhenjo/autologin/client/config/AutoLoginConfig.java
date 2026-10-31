package com.monhenjo.autologin.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything, in one Cloth Config screen: behaviour, saved logins, custom rules.
 *
 * <p>Saved logins are rows of {@code label|server|password} - the list in the screen is the
 * database, so there is no second store to drift out of sync and no custom GUI to maintain.
 * The cost is that the config file holds passwords in plain text; the file is written
 * owner-only and the screen's tooltip says so.
 */
@Config(name = "autologin")
public class AutoLoginConfig implements ConfigData {
	@ConfigEntry.Gui.Tooltip
	public boolean enabled = true;

	@ConfigEntry.Gui.Tooltip
	public boolean autoLogin = true;

	@ConfigEntry.Gui.Tooltip
	public String preferredLogin = "";

	@ConfigEntry.Gui.Tooltip
	public List<String> accounts = new ArrayList<>();

	@ConfigEntry.Gui.Tooltip
	public boolean chatList = true;

	@ConfigEntry.Gui.Tooltip
	public boolean detectInSingleplayer = false;

	@ConfigEntry.Gui.Tooltip
	public boolean debug = false;

	@ConfigEntry.Gui.Tooltip
	public List<String> customRules = new ArrayList<>();

	@Override
	public void validatePostLoad() throws ValidationException {
		if (accounts == null) {
			accounts = new ArrayList<>();
		}

		if (customRules == null) {
			customRules = new ArrayList<>();
		}

		if (preferredLogin == null) {
			preferredLogin = "";
		}

		accounts.removeIf(row -> row == null || row.isBlank());
		customRules.removeIf(rule -> rule == null || rule.isBlank());
	}
}
