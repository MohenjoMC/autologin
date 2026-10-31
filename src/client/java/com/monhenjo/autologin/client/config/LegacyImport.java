package com.monhenjo.autologin.client.config;

import com.monhenjo.autologin.AutoLogin;
import com.monhenjo.autologin.core.Account;
import com.monhenjo.autologin.core.AccountLine;
import com.monhenjo.autologin.core.AccountStore;
import com.monhenjo.autologin.storage.VaultAccountStore;

import java.nio.file.Path;
import java.util.List;

/**
 * One-time move from the encrypted vault (before 1.0.0-test.5) to the rows in the config file.
 *
 * <p>Only runs when the config has no accounts yet, so it cannot resurrect deleted logins or
 * duplicate rows on every start. The old vault file is left where it is.
 */
public final class LegacyImport {
	private LegacyImport() {
	}

	public static int importInto(AutoLoginConfig config, AccountStore store, Path configDirectory) {
		if (!config.accounts.isEmpty()) {
			return 0;
		}

		VaultAccountStore vault = VaultAccountStore.open(configDirectory);
		if (vault.problem().isPresent()) {
			return 0;
		}

		List<Account> accounts = vault.all();
		for (Account account : accounts) {
			config.accounts.add(AccountLine.format(account.label(), account.serverKey(), account.password()));
		}

		if (!accounts.isEmpty()) {
			Configs.save();
			AutoLogin.LOGGER.info("autologin: moved {} saved login(s) from the old encrypted vault into the config",
				accounts.size());
		}

		return accounts.size();
	}
}
