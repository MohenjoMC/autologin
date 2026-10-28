package com.monhenjo.autologin.client.keys;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.AutoLoginClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * One keybind: open the picker. Bound to F9 by default (free in vanilla), changeable in
 * Options > Controls like any other key.
 *
 * <p>26.x renamed the Fabric helper from {@code KeyBindingHelper} to {@code KeyMappingHelper},
 * and does not put LWJGL on the compile classpath, hence the numeric key: 298 is GLFW_KEY_F9.
 */
public final class AutoLoginKeys {
	/** GLFW_KEY_F9. */
	private static final int KEY_F9 = 298;

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AutoLogin.id("main"));

	private static KeyMapping openPicker;

	private AutoLoginKeys() {
	}

	public static void register() {
		openPicker = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.autologin.picker",
			KEY_F9,
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
