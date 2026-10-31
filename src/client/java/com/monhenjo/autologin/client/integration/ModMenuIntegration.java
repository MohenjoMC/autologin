package com.monhenjo.autologin.client.integration;

import com.monhenjo.autologin.client.config.Configs;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu opens the Cloth Config screen directly - settings and saved logins in one place.
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return Configs::screen;
	}
}
