package com.monhenjo.autologin.core;

/**
 * What kind of thing a server message is asking for.
 */
public enum LoginKind {
	/** "Please login with /login &lt;password&gt;". */
	LOGIN,
	/** "Please register with /register &lt;password&gt; &lt;password&gt;". */
	REGISTER,
	/** "You are now logged in" - the server says the session is authenticated. */
	AUTHENTICATED
}
