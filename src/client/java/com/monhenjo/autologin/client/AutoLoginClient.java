package com.monhenjo.autologin.client;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.command.AutoLoginCommand;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.Configs;
import com.monhenjo.autologin.client.keys.AutoLoginKeys;
import com.monhenjo.autologin.storage.VaultAccountStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class AutoLoginClient implements ClientModInitializer {
	private static AutoLoginRuntime runtime;

	@Override
	public void onInitializeClient() {
		Configs.init();

		VaultAccountStore store = VaultAccountStore.open(
			FabricLoader.getInstance().getConfigDir().resolve(AutoLogin.MOD_ID));

		store.problem().ifPresent(problem -> AutoLogin.LOGGER.warn("autologin: {}", problem));

		runtime = new AutoLoginRuntime(store);

		AutoLoginCommand.register(runtime);
		AutoLoginKeys.register();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			client.execute(() -> runtime.onJoin(ClientCompat.currentServerKey())));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			client.execute(runtime::onDisconnect));
	}

	public static AutoLoginRuntime runtime() {
		return runtime;
	}

	/** Called from the packet mixin for every server message; may arrive on the netty thread. */
	public static void onServerText(String text) {
		AutoLoginRuntime current = runtime;
		if (current == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.isSameThread()) {
			current.onServerText(text);
		} else {
			// handleSystemChat is hit once on the netty thread and again on the client thread.
			// Chat rendering and config access belong to the client thread; the flow dedupes the
			// resulting duplicate.
			client.execute(() -> current.onServerText(text));
		}
	}
}
