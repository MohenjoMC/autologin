package com.monhenjo.autologin.gametest;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * A stand-in for an authentication plugin, in the gametest mod only.
 *
 * <p>It registers a real server command, so the client's command tree contains /login, the mod
 * sends a genuine {@code ServerboundChatCommandPacket}, and the assertion is on the argument the
 * server actually received - not on what the client thinks it sent.
 */
public final class TestLoginCommand implements ModInitializer {
	private static volatile String lastPassword = "";
	private static volatile int logins;
	private static volatile int registrations;

	public static String lastPassword() {
		return lastPassword;
	}

	public static int logins() {
		return logins;
	}

	public static int registrations() {
		return registrations;
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("login")
				.then(argument("password", StringArgumentType.string())
					.executes(context -> {
						lastPassword = StringArgumentType.getString(context, "password");
						logins++;

						context.getSource().getPlayerOrException()
							.sendSystemMessage(Component.literal("Successfully logged in!"));

						return 1;
					}))));

		// A registration that behaves like AuthMe: two arguments, and the success wording the mod
		// treats as "authenticated".
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("register")
				.then(argument("password", StringArgumentType.string())
					.then(argument("confirm", StringArgumentType.string())
						.executes(context -> {
							lastPassword = StringArgumentType.getString(context, "password");
							registrations++;

							context.getSource().getPlayerOrException()
								.sendSystemMessage(Component.literal("Successfully registered!"));

							return 1;
						})))));
	}
}
