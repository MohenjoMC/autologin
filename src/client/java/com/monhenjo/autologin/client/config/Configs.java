package com.monhenjo.autologin.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.gui.screens.Screen;

/**
 * Cloth Config's entry point for this mod: registration happens once, everything else asks for
 * the live object.
 */
public final class Configs {
	private static ConfigHolder<AutoLoginConfig> holder;

	private Configs() {
	}

	public static synchronized void init() {
		if (holder == null) {
			holder = AutoConfig.register(AutoLoginConfig.class, GsonConfigSerializer::new);
		}
	}

	/** The live config, or a default instance if something is asked before client init. */
	public static AutoLoginConfig get() {
		ConfigHolder<AutoLoginConfig> current = holder;
		if (current == null) {
			init();
			current = holder;
		}

		AutoLoginConfig config = current.getConfig();
		return config == null ? new AutoLoginConfig() : config;
	}

	public static void save() {
		ConfigHolder<AutoLoginConfig> current = holder;
		if (current != null) {
			current.save();
		}
	}

	/** The Cloth-generated settings screen, parented to the screen it was opened from. */
	public static Screen screen(Screen parent) {
		init();
		return AutoConfigClient.getConfigScreen(AutoLoginConfig.class, parent).get();
	}
}
