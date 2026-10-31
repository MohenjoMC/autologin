package com.monhenjo.autologin.client.mixin;

import com.monhenjo.autologin.client.AutoLoginClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds server messages into the detector.
 *
 * <p>Every hook is at HEAD and none cancels: the mod only observes.
 *
 * <p>Six entry points, because a login prompt travels by whatever route the plugin felt like
 * using and each route is a different packet:
 *
 * <ul>
 *   <li>{@code handleSystemChat} - the normal {@code sendMessage} path, chat or action bar</li>
 *   <li>{@code handlePlayerChat} / {@code handleDisguisedChat} - chat-shaped broadcasts such as
 *       {@code /say}, which are <em>not</em> system chat</li>
 *   <li>{@code setActionBarText}, {@code setTitleText}, {@code setSubtitleText} - plugins that
 *       shout the prompt across the screen</li>
 * </ul>
 *
 * Some plugins send the same prompt through several routes, which is why {@code LoginFlow}
 * dedupes repeats.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleSystemChat", at = @At("HEAD"))
	private void autologin$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		AutoLoginClient.onServerText(packet.content().getString());
	}

	@Inject(method = "handlePlayerChat", at = @At("HEAD"))
	private void autologin$onPlayerChat(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
		AutoLoginClient.onServerText(packet.body().content());
	}

	@Inject(method = "handleDisguisedChat", at = @At("HEAD"))
	private void autologin$onDisguisedChat(ClientboundDisguisedChatPacket packet, CallbackInfo ci) {
		AutoLoginClient.onServerText(packet.message().getString());
	}

	@Inject(method = "setActionBarText", at = @At("HEAD"))
	private void autologin$onActionBarText(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		AutoLoginClient.onServerText(packet.text().getString());
	}

	@Inject(method = "setTitleText", at = @At("HEAD"))
	private void autologin$onTitleText(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
		AutoLoginClient.onServerText(packet.text().getString());
	}

	@Inject(method = "handleCommands", at = @At("TAIL"))
	private void autologin$onCommands(ClientboundCommandsPacket packet, CallbackInfo ci) {
		AutoLoginClient.onCommands(((ClientPacketListener) (Object) this).getCommands());
	}

	@Inject(method = "setSubtitleText", at = @At("HEAD"))
	private void autologin$onSubtitleText(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
		AutoLoginClient.onServerText(packet.text().getString());
	}
}
