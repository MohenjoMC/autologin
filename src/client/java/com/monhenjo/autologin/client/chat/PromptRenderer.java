package com.monhenjo.autologin.client.chat;

import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.LoginFlow;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The fallback picker in chat: server on one line, numbered options underneath.
 *
 * <p>Only shown when auto-login is off or cannot decide. The layout stays two lines however many
 * logins exist, because the plugin repeats its own prompt every few seconds and a taller block
 * would push the options off screen.
 */
public final class PromptRenderer {
	private PromptRenderer() {
	}

	public static void show(AutoLoginRuntime runtime, LoginFlow.Decision decision, String serverKey) {
		ClientCompat.chat(ChatTheme.header(ServerKey.display(serverKey)));

		if (decision.options().isEmpty()) {
			MutableComponent none = Component.translatable("autologin.chat.no-accounts")
				.withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal("   "))
				.append(ChatTheme.option("+", "settings", "/autologin config",
					ChatFormatting.BLUE, Component.translatable("autologin.chat.configure.tip")));

			ClientCompat.chat(none);
			return;
		}

		MutableComponent row = Component.empty();
		int index = 1;

		for (Account account : decision.options()) {
			row.append(ChatTheme.option(
				String.valueOf(index),
				account.displayName(),
				"/autologin use " + index,
				ChatFormatting.BLUE,
				Component.translatable("autologin.chat.use.tip", account.displayName(),
					ServerKey.display(serverKey))
			));
			row.append(Component.literal("   "));
			index++;
		}

		row.append(ChatTheme.option("+", "settings", "/autologin config",
			ChatFormatting.BLUE, Component.translatable("autologin.chat.configure.tip")));
		row.append(Component.literal("   "));
		row.append(ChatTheme.option("x", "later", "/autologin dismiss",
			ChatFormatting.BLUE, Component.translatable("autologin.chat.later.tip")));

		ClientCompat.chat(row);

		if (decision.retry()) {
			ClientCompat.chat(ChatTheme.state(Component.translatable("autologin.chat.retry"),
				ChatFormatting.RED));
		}
	}
}
