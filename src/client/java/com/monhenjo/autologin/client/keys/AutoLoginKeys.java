package com.monhenjo.autologin.client.keys;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.AutoLoginClient;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.Configs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * F9 opens the (Cloth) config screen, which is also where saved logins live.
 */
public final class AutoLoginKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AutoLogin.id("main"));

	private static KeyMapping openConfig;

	private AutoLoginKeys() {
	}

	public static void register() {
		openConfig = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.autologin.config",
			GLFW.GLFW_KEY_F9,
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
