package com.monhenjo.autologin.client.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * One place for how the mod talks in chat.
 *
 * <p>The shape is always the same: a gold {@code AutoLogin}, a dim separator, then one short
 * statement in grey. Actions are the only coloured words, so the eye lands on what can be clicked
 * rather than on the mod's name. No sentence repeats "AutoLogin" because the prefix already says
 * it.
 */
public final class ChatTheme {
	/**
	 * Two plain spaces. The default Minecraft font only has ASCII, so typographic punctuation
	 * (middle dot, em dash) renders as a gap or falls back to another typeface mid-line.
	 */
	private static final String SEPARATOR = "  ";

	private ChatTheme() {
	}

	/** {@code AutoLogin} in gold, bold, followed by two spaces. */
	public static MutableComponent prefix() {
		return Component.literal("AutoLogin").withStyle(ChatFormatting.GOLD)
			.withStyle(style -> style.withBold(true))
			.append(Component.literal(SEPARATOR));
	}

	/** {@code AutoLogin  <server>} - what the player is looking at. */
	public static MutableComponent header(String server) {
		return prefix().append(Component.literal(server).withStyle(ChatFormatting.WHITE));
	}

	/** {@code AutoLogin  <statement>} in grey: state, not an instruction. */
	public static MutableComponent note(Component body) {
		return prefix().append(body.copy().withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent note(String translationKey, Object... args) {
		return note(Component.translatable(translationKey, args));
	}

	/** {@code AutoLogin  <statement>} in a colour that carries meaning (green, red). */
	public static MutableComponent state(Component body, ChatFormatting colour) {
		return prefix().append(body.copy().withStyle(colour));
	}

	public static MutableComponent state(String translationKey, ChatFormatting colour, Object... args) {
		return state(Component.translatable(translationKey, args), colour);
	}

	/**
	 * A clickable token: dim brackets, a coloured key, a white label. Reads as one unit at a
	 * glance and stays legible next to the plugin's own coloured text.
	 */
	public static MutableComponent option(String key, String label, String command,
			ChatFormatting keyColour, Component tooltip) {
		return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
			.append(Component.literal(key).withStyle(keyColour))
			.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(label).withStyle(ChatFormatting.WHITE))
			.withStyle(style -> style
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(tooltip)));
	}
}
