package com.monhenjo.autologin.client;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.command.AliasSuggestions;
import com.monhenjo.autologin.client.command.AutoLoginCommand;
import com.monhenjo.autologin.client.command.LoginAliasCommands;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.AutoLoginConfig;
import com.monhenjo.autologin.client.config.ConfigAccountStore;
import com.monhenjo.autologin.client.config.Configs;
import com.monhenjo.autologin.client.config.LegacyImport;
import com.monhenjo.autologin.client.keys.AutoLoginKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;

public class AutoLoginClient implements ClientModInitializer {
	private static AutoLoginRuntime runtime;

	@Override
	public void onInitializeClient() {
		Configs.init();

		AutoLoginConfig config = Configs.get();
		ConfigAccountStore store = new ConfigAccountStore(config);
		LegacyImport.importInto(config, store,
			FabricLoader.getInstance().getConfigDir().resolve(AutoLogin.MOD_ID));

		runtime = new AutoLoginRuntime(store);

		AutoLoginCommand.register(runtime);
		LoginAliasCommands.register(runtime);
		AutoLoginKeys.register();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			client.execute(() -> runtime.onJoin(ClientCompat.currentServerKey())));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			client.execute(runtime::onDisconnect));
	}

	public static AutoLoginRuntime runtime() {
		return runtime;
	}

	/**
	 * The server's command tree just arrived (or was refreshed). Saved labels are added to the
	 * login commands' argument node here, because that tree - not ours - is what the chat
	 * suggestion popup reads.
	 */
	public static void onCommands(CommandDispatcher<?> dispatcher) {
		AutoLoginRuntime current = runtime;
		if (current == null || dispatcher == null) {
			return;
		}

		AliasSuggestions.attach(dispatcher, () -> current.suggestLabels(""));
	}

	/** Called from the packet mixin for every command the client sends. */
	public static void onOutgoingCommand(String commandLine) {
		AutoLoginRuntime current = runtime;
		if (current != null) {
			current.onOutgoingCommand(commandLine);
		}
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
			// Chat output and config access belong to the client thread; the flow dedupes the
			// resulting duplicate.
			client.execute(() -> current.onServerText(text));
		}
	}
}
