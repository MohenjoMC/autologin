package com.monhenjo.autologin.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the player can change, rendered by Cloth Config.
 *
 * <p>Saved logins are deliberately not here: they live in the encrypted vault, not in the plain
 * JSON that Cloth writes, and they are managed from the picker instead.
 */
@Config(name = "autologin")
public class AutoLoginConfig implements ConfigData {
	@ConfigEntry.Gui.Tooltip
	public boolean enabled = true;

	@ConfigEntry.Gui.Tooltip
	public boolean chatList = true;

	@ConfigEntry.Gui.Tooltip
	public boolean openScreenOnPrompt = false;

	@ConfigEntry.Gui.Tooltip
	public boolean detectInSingleplayer = false;

	@ConfigEntry.Gui.Tooltip
	public boolean debug = false;

	@ConfigEntry.Gui.Tooltip
	public List<String> customRules = new ArrayList<>();

	@Override
	public void validatePostLoad() throws ValidationException {
		if (customRules == null) {
			customRules = new ArrayList<>();
		}

		customRules.removeIf(rule -> rule == null || rule.isBlank());
	}
}
