package com.monhenjo.autologin.gametest;

import com.monhenjo.autologin.client.AutoLoginClient;
import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.AutoLoginConfig;
import com.monhenjo.autologin.client.config.Configs;
import com.monhenjo.autologin.client.gui.AccountEditScreen;
import com.monhenjo.autologin.client.gui.PickerScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The whole loop, in a real client with a real integrated server:
 *
 * <ol>
 *   <li>the server says "Please login with the command: /login &lt;password&gt;"</li>
 *   <li>the mod detects it, offers the saved login, and sends nothing yet</li>
 *   <li>the player picks it (the same call the chat link and the picker button make)</li>
 *   <li>the server's own /login handler receives exactly the saved password</li>
 *   <li>"Successfully logged in!" flips the flow to authenticated</li>
 * </ol>
 *
 * The server side is the gametest mod's {@link TestLoginCommand}, not a fake: the password really
 * travels as a command packet over the integrated connection.
 */
public class AutoLoginClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Software GL plus the default 120 fps cap starves the integrated server, and the world
		// load then times out before the test body ever runs. These are the same values the
		// screenshot still looks fine at.
		context.runOnClient(client -> {
			client.options.framerateLimit().set(20);
			client.options.renderDistance().set(4);
			client.options.simulationDistance().set(4);
		});
		context.waitTicks(2);

		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.setUseConsistentSettings(true)
			.create()) {
			context.runOnClient(client -> {
				AutoLoginRuntime runtime = AutoLoginClient.runtime();
				runtime.store().all().forEach(account -> runtime.store().remove(account.id()));

				AutoLoginConfig config = Configs.get();
				config.enabled = true;
				config.chatList = true;
				config.openScreenOnPrompt = false;
				config.detectInSingleplayer = true;
				config.debug = true;

				runtime.addAccount("main", "hunter2", "");
			});

			context.waitTicks(20);
			context.takeScreenshot("autologin-start");

			singleplayer.getServer().runCommand("say Please login with the command: /login <password>");
			context.waitTicks(20);

			int offered = context.computeOnClient(client -> AutoLoginClient.runtime().offered().size());
			check(offered == 1, "the prompt should offer exactly the saved login, got " + offered);
			check(context.computeOnClient(client -> AutoLoginClient.runtime().flow().awaitingCredentials()),
				"the prompt was not detected at all");

			String beforeClick = context.computeOnClient(client -> AutoLoginClient.runtime().lastSent());
			check(beforeClick.isEmpty(), "nothing may be sent before the player picks, sent: " + beforeClick);

			context.takeScreenshot("autologin-prompt");

			context.runOnClient(client -> AutoLoginClient.runtime().use(1));
			context.waitTicks(20);

			String sent = context.computeOnClient(client -> AutoLoginClient.runtime().lastSent());
			check("/login <hidden>".equals(sent), "client should record a masked /login send, got: " + sent);
			check("hunter2".equals(TestLoginCommand.lastPassword()),
				"the server received: " + TestLoginCommand.lastPassword());

			boolean authenticated = context.computeOnClient(client -> AutoLoginClient.runtime().flow().authenticated());
			check(authenticated, "'Successfully logged in!' should end the prompt cycle");

			context.takeScreenshot("autologin-sent");

			context.runOnClient(client -> AutoLoginClient.runtime().openPicker());
			context.waitForScreen(PickerScreen.class);
			context.takeScreenshot("autologin-picker");

			// The two other screens nobody would otherwise see in a test: the Cloth-generated
			// settings and the add-a-login form.
			context.runOnClient(client -> ClientCompat.openScreen(Configs.screen(null)));
			context.waitTicks(5);
			context.takeScreenshot("autologin-settings");

			context.runOnClient(client -> ClientCompat.openScreen(new AccountEditScreen(null, AutoLoginClient.runtime(), "")));
			context.waitForScreen(AccountEditScreen.class);
			context.takeScreenshot("autologin-add");
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
