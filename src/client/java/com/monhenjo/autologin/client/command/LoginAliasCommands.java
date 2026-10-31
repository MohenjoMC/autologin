package com.monhenjo.autologin.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.monhenjo.autologin.client.AutoLoginRuntime;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Maps the server's own authentication commands onto saved logins, so the suggestion popup that
 * appears while typing {@code /login} lists them:
 *
 * <pre>
 * /login            → send the preferred (or first) saved login
 * /login test       → send the password saved as "test"
 * /login hunter2    → not a saved label: forwarded to the server untouched
 * /reg, /register   → same, with the password repeated the way register wants it
 * </pre>
 *
 * <p>Registering these as <em>client</em> commands shadows the server's, which is the only way to
 * contribute suggestions to them. That is why anything that is not a saved label is forwarded
 * verbatim: a player typing their real password must still reach the server.
 */
public final class LoginAliasCommands {
	private LoginAliasCommands() {
	}

	public static void register(AutoLoginRuntime runtime) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
			for (String verb : new String[] {"login", "l"}) {
				dispatcher.register(command(verb, runtime));
			}

			for (String verb : new String[] {"register", "reg"}) {
				dispatcher.register(command(verb, runtime));
			}
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> command(String verb, AutoLoginRuntime runtime) {
		return LiteralArgumentBuilder.<FabricClientCommandSource>literal(verb)
			.executes(context -> {
				runtime.loginWithAlias(verb, "");
				return 1;
			})
			.then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("account",
					StringArgumentType.greedyString())
				.suggests((context, builder) -> {
					for (String label : runtime.suggestLabels(builder.getRemaining())) {
						builder.suggest(label);
					}

					return builder.buildFuture();
				})
				.executes(context -> {
					runtime.loginWithAlias(verb, StringArgumentType.getString(context, "account"));
					return 1;
				}));
	}
}
