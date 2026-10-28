package com.monhenjo.autologin.client.keys;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.AutoLoginClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * One keybind: open the picker. Bound to F9 by default (free in vanilla), changeable in
 * Options > Controls like any other key.
 */
public final class AutoLoginKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AutoLogin.id("main"));

	private static KeyMapping openPicker;

	private AutoLoginKeys() {
	}

	public static void register() {
		openPicker = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.autologin.picker",
			GLFW.GLFW_KEY_F9,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openPicker.consumeClick()) {
				if (AutoLoginClient.runtime() != null) {
					AutoLoginClient.runtime().openPicker();
				}
			}
		});
	}
}
