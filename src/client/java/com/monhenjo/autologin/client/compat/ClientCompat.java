package com.monhenjo.autologin.client.compat;

import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/**
 * The Minecraft-facing surface that actually differs between versions.
 *
 * <p>Everything else in the client package is written against APIs that have not moved since
 * 1.21.11. When a future release renames something, this is the file to fix - the compiler points
 * here, not at every call site.
 */
public final class ClientCompat {
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
		ChatComponent chat = Minecraft.getInstance().gui.getChat();
		chat.addMessage(component);
	}

	/**
	 * Opens a screen on the next client tick.
	 *
	 * <p>Not immediate on purpose: commands run from the chat box are executed inside
	 * {@code ChatScreen.sendCommand}, and the chat screen closes itself right after that call
	 * returns - which would wipe a screen opened synchronously. Deferring costs one tick and is
	 * invisible.
	 */
	public static void openScreen(Screen screen) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> client.setScreen(screen));
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
}
