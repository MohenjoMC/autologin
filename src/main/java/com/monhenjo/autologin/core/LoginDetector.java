package com.monhenjo.autologin.core;

import java.util.Optional;

/**
 * Turns the plain text of a server message into a {@link LoginPrompt}.
 *
 * <p>This is the whole external interface of detection: one string in, one answer out. The
 * pattern tables, the ordering of rules and the verb allowlist all live behind it, and are
 * exercised through this method by the unit tests.
 */
public interface LoginDetector {
	Optional<LoginPrompt> detect(String plainText);

	LoginDetector DISABLED = text -> Optional.empty();
}
