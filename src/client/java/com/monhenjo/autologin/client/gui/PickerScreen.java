package com.monhenjo.autologin.client.gui;

import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.Configs;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The screen fallback for the chat list: one button per saved login.
 *
 * <p>Deliberately built from stock widgets only - no custom rendering, no list widget - because
 * the screen rendering pipeline diverged between 1.21.11 and 26.x while buttons and text fields
 * did not.
 */
public class PickerScreen extends Screen {
	private static final int BUTTON_WIDTH = 300;
	private static final int ROW = 24;

	private final @Nullable Screen parent;
	private final AutoLoginRuntime runtime;

	public PickerScreen(@Nullable Screen parent, AutoLoginRuntime runtime) {
		super(Component.translatable("autologin.screen.picker"));
		this.parent = parent;
		this.runtime = runtime;
	}

	@Override
	protected void init() {
		int left = (this.width - BUTTON_WIDTH) / 2;
		int y = 28;

		addRenderableWidget(new StringWidget(0, 12, this.width, 12, this.title, this.font));

		List<Account> accounts = runtime.offered().isEmpty() ? runtime.store().all() : runtime.offered();

		for (Account account : accounts) {
			Component label = Component.translatable("autologin.button.account",
				account.displayName(), ServerKey.display(account.serverKey()));

			addRenderableWidget(Button.builder(label, button -> {
				runtime.useId(account.id());
				onClose();
			}).bounds(left, y, BUTTON_WIDTH, 20).build());

			y += ROW;
		}

		if (accounts.isEmpty()) {
			addRenderableWidget(new StringWidget(0, y, this.width, 12,
				Component.translatable("autologin.screen.picker.empty"), this.font));
			y += ROW;
		}

		int halfWidth = (BUTTON_WIDTH - 8) / 2;

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.add"), button ->
			ClientCompat.openScreen(new AccountEditScreen(this, runtime, runtime.serverKey()))
		).bounds(left, y + 8, halfWidth, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.settings"), button ->
			ClientCompat.openScreen(Configs.screen(this))
		).bounds(left + halfWidth + 8, y + 8, halfWidth, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.done"), button -> onClose())
			.bounds(left, y + 8 + ROW, BUTTON_WIDTH, 20).build());
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
