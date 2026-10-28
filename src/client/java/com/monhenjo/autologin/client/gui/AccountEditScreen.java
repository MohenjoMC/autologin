package com.monhenjo.autologin.client.gui;

import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.core.LoginVerbs;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Add one saved login. Two text fields and two buttons, all stock widgets.
 *
 * <p>The password field is not masked on purpose: this screen exists because the player is
 * entering their own password, and a masked field with no confirm box is how typos become
 * support tickets. Nothing here is logged.
 */
public class AccountEditScreen extends Screen {
	private static final int FIELD_WIDTH = 240;
	private static final int GAP = 26;

	private final @Nullable Screen parent;
	private final AutoLoginRuntime runtime;
	private final String serverHint;

	private EditBox labelBox;
	private EditBox passwordBox;
	private EditBox serverBox;
	private StringWidget status;

	public AccountEditScreen(@Nullable Screen parent, AutoLoginRuntime runtime, String serverKey) {
		super(Component.translatable("autologin.screen.edit"));
		this.parent = parent;
		this.runtime = runtime;
		this.serverHint = serverKey;
	}

	@Override
	protected void init() {
		int left = (this.width - FIELD_WIDTH) / 2;
		int y = 24;

		addRenderableWidget(new StringWidget(0, y, this.width, 12, this.title, this.font));
		y += GAP;

		addRenderableWidget(new StringWidget(left, y, FIELD_WIDTH, 12, Component.translatable("autologin.field.label"), this.font));
		y += 12;
		labelBox = new EditBox(this.font, left, y, FIELD_WIDTH, 20, Component.translatable("autologin.field.label"));
		labelBox.setMaxLength(32);
		labelBox.setHint(Component.translatable("autologin.field.label.hint"));
		addRenderableWidget(labelBox);
		y += GAP;

		addRenderableWidget(new StringWidget(left, y, FIELD_WIDTH, 12, Component.translatable("autologin.field.password"), this.font));
		y += 12;
		passwordBox = new EditBox(this.font, left, y, FIELD_WIDTH, 20, Component.translatable("autologin.field.password"));
		passwordBox.setMaxLength(128);
		passwordBox.setHint(Component.translatable("autologin.field.password.hint"));
		addRenderableWidget(passwordBox);
		y += GAP;

		addRenderableWidget(new StringWidget(left, y, FIELD_WIDTH, 12, Component.translatable("autologin.field.server"), this.font));
		y += 12;
		serverBox = new EditBox(this.font, left, y, FIELD_WIDTH, 20, Component.translatable("autologin.field.server"));
		serverBox.setMaxLength(128);
		serverBox.setHint(Component.translatable("autologin.field.server.hint"));
		serverBox.setValue(serverHint == null ? "" : serverHint);
		addRenderableWidget(serverBox);
		y += GAP;

		status = new StringWidget(left, y, FIELD_WIDTH, 12, Component.empty(), this.font);
		addRenderableWidget(status);
		y += 14;

		int halfWidth = (FIELD_WIDTH - 8) / 2;

		addRenderableWidget(Button.builder(Component.translatable("autologin.button.save"), button -> save())
			.bounds(left, y, halfWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("autologin.button.cancel"), button -> onClose())
			.bounds(left + halfWidth + 8, y, halfWidth, 20).build());

		setInitialFocus(labelBox);
	}

	private void save() {
		String password = passwordBox.getValue();
		if (!LoginVerbs.isValidPassword(password)) {
			status.setMessage(Component.translatable("autologin.error.password"));
			return;
		}

		String server = serverBox.getValue() == null || serverBox.getValue().isBlank()
			? runtime.serverKey()
			: ServerKey.normalize(serverBox.getValue());

		try {
			runtime.addAccount(labelBox.getValue(), password, server);
		} catch (IllegalArgumentException e) {
			status.setMessage(Component.translatable("autologin.error.password"));
			return;
		}

		onClose();
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
