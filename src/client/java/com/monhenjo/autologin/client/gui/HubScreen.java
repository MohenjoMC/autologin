package com.monhenjo.autologin.client.gui;

import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.Configs;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What Mod Menu opens: a two-button hub, because Cloth Config's screen is for settings and the
 * saved logins need a list of their own.
 */
public class HubScreen extends Screen {
	private final @Nullable Screen parent;
	private final AutoLoginRuntime runtime;

	public HubScreen(@Nullable Screen parent, AutoLoginRuntime runtime) {
		super(Component.translatable("autologin.screen.hub"));
		this.parent = parent;
		this.runtime = runtime;
	}

	@Override
	protected void init() {
		int left = (this.width - 200) / 2;
		int y = this.height / 2 - 40;

		addRenderableWidget(new StringWidget(0, y - 24, this.width, 12, this.title, this.font));

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.logins"), button ->
			ClientCompat.openScreen(new PickerScreen(this, runtime))
		).bounds(left, y, 200, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.settings"), button ->
			ClientCompat.openScreen(Configs.screen(this))
		).bounds(left, y + 24, 200, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.done"), button -> onClose())
			.bounds(left, y + 48, 200, 20).build());
	}

	@Override
	public void onClose() {
		if (parent != null) {
			ClientCompat.openScreen(parent);
		} else {
			super.onClose();
		}
	}
}
