package com.monhenjo.autologin.client.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * How the mod talks in chat.
 *
 * <p>Blue marks what belongs to the mod (its name, the key inside each bracket). Light grey is
 * everything you read but cannot click. Brackets come back because they make the clickable tokens
 * obvious at a glance without spending colour on them.
 *
 * <p>Spacing is plain ASCII: the default font has no typographic punctuation, so a middle dot
 * renders as a gap.
 */
public final class ChatTheme {
	private static final String SEPARATOR = "  ";
	private static final ChatFormatting ACCENT = ChatFormatting.BLUE;
	private static final ChatFormatting TEXT = ChatFormatting.GRAY;

	private ChatTheme() {
	}

	/** {@code [autologin]} - dim brackets, lowercase name in blue, then two spaces. */
	public static MutableComponent prefix() {
		return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
			.append(Component.literal("autologin").withStyle(ACCENT))
			.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(SEPARATOR));
	}

	/** {@code AutoLogin  <server>}: the address in light grey. */
	public static MutableComponent header(String server) {
		return prefix().append(Component.literal(server).withStyle(TEXT));
	}

	/** {@code AutoLogin  <statement>} in light grey. */
	public static MutableComponent note(Component body) {
		return prefix().append(body.copy().withStyle(TEXT));
	}

	public static MutableComponent note(String translationKey, Object... args) {
		return note(Component.translatable(translationKey, args));
	}

	/** Same, in a colour that carries meaning (green for saved, red for failed). */
	public static MutableComponent state(Component body, ChatFormatting colour) {
		return prefix().append(body.copy().withStyle(colour));
	}

	public static MutableComponent state(String translationKey, ChatFormatting colour, Object... args) {
		return state(Component.translatable(translationKey, args), colour);
	}

	/**
	 * A clickable token: dim brackets, a blue key, a light grey label.
	 * {@code [1] main} reads as one unit and the label stays the calm part.
	 */
	public static MutableComponent option(String key, String label, String command,
			ChatFormatting keyColour, Component tooltip) {
		return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
			.append(Component.literal(key).withStyle(keyColour))
			.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(label).withStyle(TEXT))
			.withStyle(style -> style
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(tooltip)));
	}
}
