package com.monhenjo.autologin.client;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.client.chat.PromptRenderer;
import com.monhenjo.autologin.client.compat.ClientCompat;
import com.monhenjo.autologin.client.config.AutoLoginConfig;
import com.monhenjo.autologin.client.config.Configs;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.AccountStore;
import com.monhenjo.autologin.core.AutoLoginChoice;
import com.monhenjo.autologin.core.LoginDetector;
import com.monhenjo.autologin.core.LoginFlow;
import com.monhenjo.autologin.core.LoginKind;
import com.monhenjo.autologin.core.LoginPrompt;
import com.monhenjo.autologin.core.LoginVerbs;
import com.monhenjo.autologin.core.PromptRules;
import com.monhenjo.autologin.core.RuleBasedDetector;
import com.monhenjo.autologin.core.ServerKey;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The seam between plain-Java login logic and the running client.
 *
 * <p>Detection, choosing and command building live in {@code core}; this class only carries the
 * few things that need the game: the current server address, the chat HUD, and the network
 * handler that actually sends the command.
 */
public final class AutoLoginRuntime {
	private final AccountStore store;
	private final LoginFlow flow = new LoginFlow();

	private LoginDetector detector = new RuleBasedDetector();
	private List<String> detectorRules = List.of();

	private String serverKey = "";
	private List<Account> offered = List.of();
	private String lastSent = "";
	private String lastError = "";

	public AutoLoginRuntime(AccountStore store) {
		this.store = store;
	}

	public AccountStore store() {
		return store;
	}

	public LoginFlow flow() {
		return flow;
	}

	public String serverKey() {
		return serverKey;
	}

	/** The accounts the last prompt offered, in the order the chat list shows them. */
	public List<Account> offered() {
		return offered;
	}

	/** "sent /login &lt;hidden&gt;", for the debug log and the gametest. Never the password. */
	public String lastSent() {
		return lastSent;
	}

	public String lastError() {
		return lastError;
	}

	public void onJoin(String address) {
		serverKey = ServerKey.normalize(address);
		offered = List.of();
		flow.onDisconnected();
		debug("joined " + ServerKey.display(serverKey));
	}

	public void onDisconnect() {
		flow.onDisconnected();
		offered = List.of();
		serverKey = "";
	}

	/**
	 * Every server message the client sees, already flattened to plain text. Called from the
	 * packet mixin, marshalled onto the client thread.
	 */
	public void onServerText(String text) {
		AutoLoginConfig config = Configs.get();
		if (!config.enabled || text == null || text.isBlank()) {
			return;
		}

		syncRules(config);

		Optional<LoginPrompt> found = detector.detect(text);
		if (found.isEmpty()) {
			return;
		}

		LoginPrompt prompt = found.get();

		if (prompt.kind() == LoginKind.AUTHENTICATED) {
			if (flow.awaitingCredentials()) {
				debug("authenticated (" + prompt.ruleId() + ")");
				flow.onAuthenticated();
			}

			return;
		}

		// A fast server can print its prompt in the same tick it finishes the join.
		if (serverKey.isEmpty()) {
			serverKey = ClientCompat.currentServerKey();
		}

		if (serverKey.isEmpty() && !config.detectInSingleplayer) {
			debug("read " + prompt.ruleId() + " but there is no server address and detectInSingleplayer is off");
			return;
		}

		List<Account> options = new ArrayList<>();
		for (Account account : store.forServer(serverKey)) {
			if (account.usable()) {
				options.add(account);
			}
		}

		debug("prompt " + prompt.ruleId() + " -> " + prompt.verb() + ", " + options.size() + " candidate(s)");

		Optional<LoginFlow.Decision> decision = flow.onPrompt(prompt, options, System.currentTimeMillis());
		if (decision.isEmpty()) {
			return;
		}

		offered = options;

		if (config.autoLogin && !options.isEmpty()) {
			Optional<Account> chosen = AutoLoginChoice.choose(config.preferredLogin, options);
			if (chosen.isPresent() && send(chosen.get())) {
				debug("auto-login as " + chosen.get().displayName());
				return;
			}
		}

		if (config.chatList) {
			PromptRenderer.show(this, decision.get(), serverKey);
		}
	}

	/** Sends the n-th account in the last prompt (1-based, as shown in chat). */
	public boolean use(int index) {
		if (index < 1 || index > offered.size()) {
			fail("autologin.chat.bad-choice");
			return false;
		}

		return useId(offered.get(index - 1).id());
	}

	public boolean useId(String accountId) {
		Optional<Account> account = store.find(accountId);
		if (account.isEmpty()) {
			fail("autologin.chat.bad-choice");
			return false;
		}

		return send(account.get());
	}

	/**
	 * What the {@code /login}, {@code /l}, {@code /register} and {@code /reg} client commands do.
	 *
	 * <p>Those commands are registered client-side so the suggestion list can offer the saved
	 * labels. Anything that is not a saved label is forwarded to the server untouched, so typing
	 * a real password by hand still works exactly as before.
	 */
	public boolean loginWithAlias(String verb, String typed) {
		List<Account> options = store.forServer(serverKey.isEmpty() ? ServerKey.ANY : serverKey);

		if (typed == null || typed.isBlank()) {
			Optional<Account> chosen = AutoLoginChoice.choose(Configs.get().preferredLogin, options);
			if (chosen.isPresent()) {
				return send(chosen.get());
			}

			list(options);
			return false;
		}

		Optional<Account> byLabel = AutoLoginChoice.matchLabel(typed, options);
		if (byLabel.isPresent()) {
			return send(byLabel.get());
		}

		return forward(verb, typed);
	}

	/** Labels for the command suggestion list. */
	public List<String> suggestLabels(String typed) {
		List<Account> options = serverKey.isEmpty() ? store.all() : store.forServer(serverKey);
		return AutoLoginChoice.suggest(typed, options);
	}

	public boolean send(Account account) {
		Optional<String> line = flow.commandFor(account);
		if (line.isEmpty()) {
			lastError = "password cannot be sent as a command";
			AutoLogin.LOGGER.warn("autologin: refusing to send {} - password is not expressible as a command", account);
			fail("autologin.chat.unsendable");
			return false;
		}

		ClientPacketListener connection = ClientCompat.connection();
		if (connection == null) {
			lastError = "not connected";
			fail("autologin.chat.not-connected");
			return false;
		}

		String command = line.get();
		String payload = command.startsWith("/") ? command.substring(1) : command;

		try {
			connection.sendCommand(payload);
		} catch (RuntimeException e) {
			lastError = e.getMessage() == null ? e.toString() : e.getMessage();
			AutoLogin.LOGGER.warn("autologin: sending {} failed: {}", LoginVerbs.describeCommand(command), lastError);
			fail("autologin.chat.not-connected");
			return false;
		}

		long now = System.currentTimeMillis();
		flow.onPasswordSent(now);
		store.markUsed(account.id(), now);
		lastSent = LoginVerbs.describeCommand(command);
		debug("sent " + lastSent + " for " + account.displayName());

		chat(Component.translatable("autologin.chat.sent", account.displayName(), ServerKey.display(serverKey)));
		return true;
	}

	/**
	 * Sends a command the player typed to the server, bypassing the client command tables (which
	 * would route it straight back here).
	 */
	private boolean forward(String verb, String typed) {
		ClientPacketListener connection = ClientCompat.connection();
		if (connection == null) {
			lastError = "not connected";
			return false;
		}

		String line = typed.isBlank() ? verb : verb + " " + typed;

		try {
			connection.send(new ServerboundChatCommandPacket(line));
		} catch (RuntimeException e) {
			lastError = e.getMessage() == null ? e.toString() : e.getMessage();
			AutoLogin.LOGGER.warn("autologin: forwarding /{} failed: {}", verb, lastError);
			return false;
		}

		debug("forwarded /" + verb + " <hidden>");
		return true;
	}

	public Account addAccount(String label, String password, String server) {
		Account account = store.add(label, password, server);

		if (flow.awaitingCredentials()) {
			offered = store.forServer(serverKey);
		}

		return account;
	}

	public void dismiss() {
		offered = List.of();
		flow.onDismissed();
	}

	public void openConfig() {
		ClientCompat.openScreen(Configs.screen(null));
	}

	public void debug(String message) {
		if (Configs.get().debug) {
			AutoLogin.LOGGER.info("autologin: {}", message);
		}
	}

	private void list(List<Account> accounts) {
		if (accounts.isEmpty()) {
			chat(Component.translatable("autologin.chat.list.empty"));
			return;
		}

		for (Account account : accounts) {
			chat(Component.translatable("autologin.chat.list.entry", account.displayName(),
				ServerKey.display(account.serverKey())));
		}
	}

	private void chat(Component component) {
		try {
			ClientCompat.chat(component);
		} catch (RuntimeException e) {
			// No chat HUD yet (title screen, very early join). Nothing to show it on.
		}
	}

	private void fail(String translationKey) {
		chat(Component.translatable(translationKey).withStyle(net.minecraft.ChatFormatting.RED));
	}

	private void syncRules(AutoLoginConfig config) {
		List<String> rules = config.customRules == null ? List.of() : List.copyOf(config.customRules);
		if (rules.equals(detectorRules)) {
			return;
		}

		detectorRules = rules;
		PromptRules.ParseResult parsed = PromptRules.parseCustom(rules);

		for (String rejected : parsed.rejected()) {
			AutoLogin.LOGGER.warn("autologin: ignoring custom rule {}", rejected);
		}

		detector = parsed.rules().isEmpty() ? new RuleBasedDetector() : new RuleBasedDetector(parsed.rules());
	}
}
