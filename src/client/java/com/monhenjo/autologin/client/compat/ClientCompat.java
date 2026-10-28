package com.monhenjo.autologin.client.compat;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The Minecraft-facing surface that actually differs between versions.
 *
 * <p>Everything else in the client package is written against APIs that have not moved across
 * 26.1 - 26.3. The one that did is the chat HUD: 26.1 keeps it on {@code Gui#getChat()}, while
 * 26.2 moved it to {@code Gui.hud} behind the same method name. One source tree has to build all
 * of them, so the lookup happens once, here, and a failure degrades to "no chat lines" instead of
 * crashing the game - the picker screen still works.
 */
public final class ClientCompat {
	private static boolean chatResolved;
	private static Object chatComponent;
	private static Method addClientSystemMessage;

	private ClientCompat() {
	}

	public static Minecraft minecraft() {
		return Minecraft.getInstance();
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	/** Adds a client-side line to the chat HUD. Click and hover events in it work like any other. */
	public static void chat(Component component) {
		Object chat = chatComponent();
		if (chat == null || addClientSystemMessage == null) {
			return;
		}

		try {
			addClientSystemMessage.invoke(chat, component);
		} catch (ReflectiveOperationException e) {
			AutoLogin.LOGGER.warn("autologin: chat HUD is not reachable: {}", e.toString());
		}
	}

	public static void openScreen(Screen screen) {
		Minecraft.getInstance().setScreenAndShow(screen);
	}

	public static ClientPacketListener connection() {
		return Minecraft.getInstance().getConnection();
	}

	/** "" on the title screen and in singleplayer; a normalised key otherwise. */
	public static String currentServerKey() {
		ServerData data = Minecraft.getInstance().getCurrentServer();
		return data == null ? "" : ServerKey.normalize(data.ip);
	}

	public static String playerName() {
		return Minecraft.getInstance().getUser() == null ? "" : Minecraft.getInstance().getUser().getName();
	}

	private static Object chatComponent() {
		if (chatResolved) {
			return chatComponent;
		}

		chatResolved = true;
		Gui gui = Minecraft.getInstance().gui;

		// 26.2+: Gui.hud.getChat()
		try {
			Field hud = Gui.class.getField("hud");
			Object hudValue = hud.get(gui);
			Object chat = hudValue.getClass().getMethod("getChat").invoke(hudValue);
			addClientSystemMessage = chat.getClass().getMethod("addClientSystemMessage", Component.class);
			chatComponent = chat;
			return chatComponent;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// Fall through to the 26.1 shape.
		}

		// 26.1: Gui.getChat()
		try {
			Object chat = Gui.class.getMethod("getChat").invoke(gui);
			addClientSystemMessage = chat.getClass().getMethod("addClientSystemMessage", Component.class);
			chatComponent = chat;
		} catch (ReflectiveOperationException | RuntimeException e) {
			AutoLogin.LOGGER.warn("autologin: no chat HUD found, prompts will only appear in the picker screen");
			chatComponent = null;
		}

		return chatComponent;
	}
}
