package com.monhenjo.autologin.client.integration;

import com.monhenjo.autologin.client.AutoLoginClient;
import com.monhenjo.autologin.client.gui.HubScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu hook. Compiled against Mod Menu, loaded only when it is present.
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new HubScreen(parent, AutoLoginClient.runtime());
	}
}
