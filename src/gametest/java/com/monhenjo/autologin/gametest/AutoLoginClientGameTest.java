package com.monhenjo.autologin.gametest;

import com.monhenjo.autologin.client.AutoLoginClient;
import com.monhenjo.autologin.client.AutoLoginRuntime;
import com.monhenjo.autologin.client.config.AutoLoginConfig;
import com.monhenjo.autologin.client.config.Configs;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The whole loop in a real client with a real integrated server, for both behaviours:
 *
 * <ol>
 *   <li>auto-login on: the prompt is answered with no click at all</li>
 *   <li>auto-login off: nothing is sent until the player picks, then "/login test" works</li>
 *   <li>typing the saved label (what the /login suggestion list offers) sends the password</li>
 * </ol>
 *
 * The server side is the gametest mod's {@link TestLoginCommand}: the password really travels as
 * a command packet over the connection.
 */
public class AutoLoginClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
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
				config.autoLogin = true;
				config.chatList = true;
				config.detectInSingleplayer = true;
				config.preferredLogin = "";
				config.debug = true;

				runtime.store().add("main", "hunter2", "");
			});

			context.waitTicks(20);
			context.takeScreenshot("autologin-start");

			// 1. auto-login: prompt arrives, password leaves without a click
			singleplayer.getServer().runCommand("say Please login with the command: /login <password>");
			context.waitTicks(25);

			int afterAuto = TestLoginCommand.logins();
			check(afterAuto == 1, "auto-login should have sent exactly one login, got " + afterAuto);
			check("hunter2".equals(TestLoginCommand.lastPassword()),
				"the server received: " + TestLoginCommand.lastPassword());
			context.takeScreenshot("autologin-auto");

			// 2. manual picker: the same prompt must not send anything by itself
			context.waitTicks(420); // past the 15s resend grace
			context.runOnClient(client -> Configs.get().autoLogin = false);

			singleplayer.getServer().runCommand("say Please login with the command: /login <password>");
			context.waitTicks(25);

			check(TestLoginCommand.logins() == afterAuto,
				"with auto-login off nothing may be sent before the player picks");
			int offered = context.computeOnClient(client -> AutoLoginClient.runtime().offered().size());
			check(offered == 1, "the prompt should offer the saved login, got " + offered);
			context.takeScreenshot("autologin-prompt");

			context.runOnClient(client -> AutoLoginClient.runtime().use(1));
			context.waitTicks(20);
			check(TestLoginCommand.logins() == afterAuto + 1, "clicking the option should send the password");

			// 3. the label typed after /login (what the suggestion list offers)
			context.waitTicks(420);
			context.runOnClient(client -> AutoLoginClient.runtime().loginWithAlias("login", "main"));
			context.waitTicks(20);
			check(TestLoginCommand.logins() == afterAuto + 2, "/login <label> should send the saved password");
			check("hunter2".equals(TestLoginCommand.lastPassword()), "the label must resolve to the saved password");

			context.takeScreenshot("autologin-alias");

			// 4. a registration typed by hand is saved once the server confirms it
			context.runOnClient(client -> {
				AutoLoginClient.runtime().store().all()
					.forEach(account -> AutoLoginClient.runtime().store().remove(account.id()));
				AutoLoginClient.runtime().flow().onDisconnected();
				Configs.get().autoLogin = false;
			});

			singleplayer.getServer().runCommand("say Please register with the command: /register <password> <ConfirmPassword>");
			context.waitTicks(20);

			context.runOnClient(client -> {
				if (client.getConnection() != null) {
					client.getConnection().sendCommand("register hunter2 hunter2");
				}
			});
			context.waitTicks(30);

			check(TestLoginCommand.registrations() == 1, "the test server should have seen the registration");
			int saved = context.computeOnClient(client -> AutoLoginClient.runtime().store().all().size());
			check(saved == 1, "the registration password should have been saved, got " + saved);
			check("hunter2".equals(context.computeOnClient(client ->
					AutoLoginClient.runtime().store().all().get(0).password())),
				"the saved password must be the one that was registered");
			context.takeScreenshot("autologin-saved");

			// 5. the Cloth screen, with its tabs
			context.runOnClient(client -> AutoLoginClient.runtime().openConfig());
			context.waitTicks(15);
			context.takeScreenshot("autologin-config");
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
