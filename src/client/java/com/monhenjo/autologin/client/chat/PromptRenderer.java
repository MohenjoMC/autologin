package com.monhenjo.autologin.client.chat;

import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.LoginFlow;
import com.monhenjo.autologin.core.LoginKind;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * The fallback picker in chat: numbered, clickable options, shown only when auto-login is off or
 * could not decide. With auto-login on this never appears - that is the point of the setting.
 */
public final class PromptRenderer {
	private PromptRenderer() {
	}

	public static void show(AutoLoginRuntime runtime, LoginFlow.Decision decision, String serverKey) {
		String headerKey = decision.prompt().kind() == LoginKind.REGISTER
			? "autologin.chat.header.register"
			: "autologin.chat.header";

		ClientCompat.chat(Component.translatable(headerKey, ServerKey.display(serverKey))
			.withStyle(ChatFormatting.GOLD));

		if (decision.options().isEmpty()) {
			ClientCompat.chat(Component.translatable("autologin.chat.no-accounts")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal("  "))
				.append(option(Component.translatable("autologin.chat.configure"), "/autologin config",
					ChatFormatting.GREEN, "autologin.chat.configure.tip")));
			return;
		}

		MutableComponent row = Component.empty();
		int index = 1;

		for (Account account : decision.options()) {
			row.append(option(Component.literal("[" + index + "] " + account.displayName()),
				"/autologin use " + index, ChatFormatting.AQUA,
				Component.translatable("autologin.chat.use.tip", account.displayName(), ServerKey.display(serverKey))));
			row.append(Component.literal("  "));
			index++;
		}

		row.append(option(Component.translatable("autologin.chat.configure"), "/autologin config",
			ChatFormatting.GREEN, "autologin.chat.configure.tip"));
		row.append(Component.literal("  "));
		row.append(option(Component.translatable("autologin.chat.later"), "/autologin dismiss",
			ChatFormatting.DARK_GRAY, "autologin.chat.later.tip"));

		ClientCompat.chat(row);

		if (decision.retry()) {
			ClientCompat.chat(Component.translatable("autologin.chat.retry").withStyle(ChatFormatting.RED));
		}
	}

	private static MutableComponent option(Component label, String command, ChatFormatting colour, String tooltipKey) {
		return option(label, command, colour, Component.translatable(tooltipKey));
	}

	private static MutableComponent option(Component label, String command, ChatFormatting colour, Component tooltip) {
		return label.copy().withStyle(style -> style
			.withColor(colour)
			.withClickEvent(new ClickEvent.RunCommand(command))
			.withHoverEvent(new HoverEvent.ShowText(tooltip)));
	}
}
