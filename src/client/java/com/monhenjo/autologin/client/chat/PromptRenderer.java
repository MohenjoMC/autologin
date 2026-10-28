package com.monhenjo.autologin.client.chat;

import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.LoginFlow;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * The in-chat picker: numbered, clickable options, the same shape as command suggestions.
 *
 * <p>Clicking a line runs a <em>client</em> command ({@code /autologin use 1}) which Fabric
 * intercepts locally. The password is only ever placed on the network by
 * {@link AutoLoginRuntime#send}, never into the chat box, so it cannot end up in chat history or
 * in a screenshot of the input line.
 */
public final class PromptRenderer {
	private PromptRenderer() {
	}

	public static void show(AutoLoginRuntime runtime, LoginFlow.Decision decision, String serverKey) {
		ClientCompat.chat(Component.translatable("autologin.chat.header", ServerKey.display(serverKey))
			.withStyle(ChatFormatting.GOLD));

		if (decision.options().isEmpty()) {
			MutableComponent empty = Component.translatable("autologin.chat.no-accounts").withStyle(ChatFormatting.GRAY);
			empty.append(Component.literal("  "));
			empty.append(option(Component.translatable("autologin.chat.add"), "/autologin add",
				ChatFormatting.GREEN, "autologin.chat.add.tip"));
			ClientCompat.chat(empty);
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

		row.append(option(Component.translatable("autologin.chat.add"), "/autologin add",
			ChatFormatting.GREEN, "autologin.chat.add.tip"));
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
