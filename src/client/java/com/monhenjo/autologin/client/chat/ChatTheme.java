package com.monhenjo.autologin.client.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * How the mod talks in chat.
 *
 * <p>Minecraft has no per-message font size, so "smaller" has to come from weight, colour and
 * glyph count. Rules here: no bold, no gold, one dark grey label at the start of the line, and the
 * only coloured words are the ones you can click. That keeps our lines quieter than the plugin's
 * own coloured chat instead of competing with it.
 *
 * <p>Spacing is plain ASCII because the default font has no typographic punctuation; a middle dot
 * renders as a gap.
 */
public final class ChatTheme {
	private static final String SEPARATOR = "  ";

	private ChatTheme() {
	}

	/** {@code AutoLogin} in dark grey, two spaces. Reads as a label, not a headline. */
	public static MutableComponent prefix() {
		return Component.literal("AutoLogin").withStyle(ChatFormatting.DARK_GRAY)
			.append(Component.literal(SEPARATOR));
	}

	/** {@code AutoLogin  <server>}. */
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

	/** Same, in a colour that carries meaning. */
	public static MutableComponent state(Component body, ChatFormatting colour) {
		return prefix().append(body.copy().withStyle(colour));
	}

	public static MutableComponent state(String translationKey, ChatFormatting colour, Object... args) {
		return state(Component.translatable(translationKey, args), colour);
	}

	/**
	 * A clickable token without brackets: a coloured key, a space, a grey label. Brackets cost two
	 * glyphs per option and read as heavier than the words inside them.
	 */
	public static MutableComponent option(String key, String label, String command,
			ChatFormatting keyColour, Component tooltip) {
		return Component.literal(key).withStyle(keyColour)
			.append(Component.literal(" "))
			.append(Component.literal(label).withStyle(ChatFormatting.GRAY))
			.withStyle(style -> style
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(tooltip)));
	}
}
