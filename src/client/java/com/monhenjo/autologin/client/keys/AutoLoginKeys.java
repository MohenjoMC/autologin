package com.monhenjo.autologin.client.keys;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.AutoLoginClient;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * F9 opens the (Cloth) config screen, which is also where saved logins live.
 *
 * <p>26.x renamed the Fabric helper from KeyBindingHelper to KeyMappingHelper and does not put
 * LWJGL on the compile classpath, hence the numeric key: 298 is GLFW_KEY_F9.
 */
public final class AutoLoginKeys {
	/** GLFW_KEY_F9. */
	private static final int KEY_F9 = 298;

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AutoLogin.id("main"));

	private static KeyMapping openConfig;

	private AutoLoginKeys() {
	}

	public static void register() {
		openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.autologin.config",
			KEY_F9,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfig.consumeClick()) {
				if (AutoLoginClient.runtime() != null) {
					ClientCompat.openScreen(Configs.screen(null));
				}
			}
		});
	}
}
