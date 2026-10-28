package com.monhenjo.autologin.client.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.Configs;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.ServerKey;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * {@code /autologin ...}, registered client-side, so it never reaches the server and never shows
 * up in the server's logs.
 *
 * <p>Note what is <em>not</em> here: there is no {@code /autologin add <password>}. A password
 * typed into chat is a password in chat history, so adding always goes through the screen.
 *
 * <p>The tree is built with Brigadier's own builders rather than Fabric's one-per-version sugar
 * helpers, so this file is identical on every supported Minecraft line.
 */
public final class AutoLoginCommand {
	private AutoLoginCommand() {
	}

	public static void register(AutoLoginRuntime runtime) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
			dispatcher.register(tree(runtime)));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> tree(AutoLoginRuntime runtime) {
		return LiteralArgumentBuilder.<FabricClientCommandSource>literal("autologin")
			.executes(context -> {
				runtime.openPicker();
				return 1;
			})
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("use")
				.then(RequiredArgumentBuilder.<FabricClientCommandSource, Integer>argument(
						"index", IntegerArgumentType.integer(1))
					.executes(context -> {
						runtime.use(IntegerArgumentType.getInteger(context, "index"));
						return 1;
					})))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("add")
				.executes(context -> {
					runtime.openAdd();
					return 1;
				}))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("list")
				.executes(context -> {
					list(runtime);
					return 1;
				}))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("remove")
				.then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
						"label", StringArgumentType.greedyString())
					.executes(context -> {
						remove(runtime, StringArgumentType.getString(context, "label"));
						return 1;
					})))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("config")
				.executes(context -> {
					ClientCompat.openScreen(Configs.screen(null));
					return 1;
				}))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("dismiss")
				.executes(context -> {
					runtime.dismiss();
					return 1;
				}))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("on")
				.executes(context -> {
					setEnabled(true);
					return 1;
				}))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("off")
				.executes(context -> {
					setEnabled(false);
					return 1;
				}))
			.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("debug")
				.executes(context -> {
					Configs.get().debug = !Configs.get().debug;
					Configs.save();
					ClientCompat.chat(Component.translatable("autologin.chat.debug",
						String.valueOf(Configs.get().debug)));
					return 1;
				}));
	}

	private static void list(AutoLoginRuntime runtime) {
		var accounts = runtime.store().all();

		if (accounts.isEmpty()) {
			ClientCompat.chat(Component.translatable("autologin.chat.list.empty").withStyle(ChatFormatting.GRAY));
			return;
		}

		ClientCompat.chat(Component.translatable("autologin.chat.list.header", String.valueOf(accounts.size()))
			.withStyle(ChatFormatting.GOLD));

		for (Account account : accounts) {
			ClientCompat.chat(Component.literal(" - " + account.displayName() + " @ "
				+ ServerKey.display(account.serverKey())).withStyle(ChatFormatting.GRAY));
		}

		runtime.store().problem().ifPresent(problem ->
			ClientCompat.chat(Component.literal(" ! " + problem).withStyle(ChatFormatting.RED)));
	}

	private static void remove(AutoLoginRuntime runtime, String label) {
		String wanted = label.trim();

		for (Account account : runtime.store().all()) {
			if (account.label().equalsIgnoreCase(wanted) || account.id().equals(wanted)) {
				runtime.store().remove(account.id());
				ClientCompat.chat(Component.translatable("autologin.chat.removed", account.displayName()));
				return;
			}
		}

		ClientCompat.chat(Component.translatable("autologin.chat.remove.not-found", wanted)
			.withStyle(ChatFormatting.RED));
	}

	private static void setEnabled(boolean enabled) {
		Configs.get().enabled = enabled;
		Configs.save();
		ClientCompat.chat(Component.translatable(enabled ? "autologin.chat.on" : "autologin.chat.off"));
	}
}
