# AGENTS.md

AutoLogin is a client-side Fabric mod: a password manager for servers that ask
you to log in. Package is `com.monhenjo.autologin`. License is CC0.

## Two branches, one project

- `main` builds the 26.x line and above. Loom plugin `net.fabricmc.fabric-loom`,
  Java 25 bytecode, Mojang-mapped jars (no remapping).
- `1.21.11` builds the legacy line. Loom plugin `net.fabricmc.fabric-loom-remap`
  with `loom.officialMojangMappings()`, Java 21 bytecode.

The gradle files differ between branches. The source layout does not. When a
change applies to both lines, land it on both branches. Only three files are
allowed to differ in `src/`:

- `client/compat/ClientCompat.java` - the Minecraft calls that moved
  (`Gui#getChat` vs `Gui.hud#getChat`, `setScreen` vs `setScreenAndShow`,
  `addMessage` vs `addClientSystemMessage`).
- `client/keys/AutoLoginKeys.java` - `KeyBindingHelper` vs `KeyMappingHelper`.
- `client/resources/autologin.client.mixins.json` - `JAVA_21` vs `JAVA_25`.

Everything else is byte-identical across branches; keep it that way.

## Build

```sh
./gradlew build gametestClasses
```

Gradle must run on Java 25 on both branches, because Loom 1.19 refuses older
JVMs. On this box that is `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`. CI
does the same through setup-java. The mod still compiles to the right bytecode,
decided by `options.release` in build.gradle (25 on main, 21 on 1.21.11).

Jars land in `build/libs/` as `autologin-<mod_version>+<minecraft_version>.jar`.

## Version matrix

One source tree builds every version. CI (`.github/workflows/build.yml`) passes
`-P` overrides, and `gradle.properties` stays at one release:

```sh
./gradlew build gametestClasses -Pminecraft_version=26.3 \
  -Pfabric_api_version=0.165.0+26.3 -Pcloth_config_version=26.3.159 \
  -Pmodmenu_version=21.0.0 -Pminecraft_dependency="~26.3"
```

`minecraft_dependency` is expanded into the `depends.minecraft` field of
`fabric.mod.json` at build time, so a 26.1 jar says `~26.1`. Hardcoding a
Minecraft version into that field breaks the matrix.

To support a new release: read its values with `fabric versions <mc> --json`,
pick the matching Cloth Config (`me.shedaniel.cloth:cloth-config-fabric`, version
follows the MC line) and Mod Menu from their mavens, then add one matrix entry.

## Where code goes

- `src/main` is **pure Java on purpose**: detection rules, the account model, the
  login flow state machine, and the encrypted vault. No Minecraft imports. This
  is the part that gets unit-tested without booting the game, and the part that
  ports across versions untouched.
- `src/client` is the Minecraft side: the packet mixin, chat rendering, screens,
  the client command, the keybind, the Cloth config, Mod Menu glue.
- `src/gametest` is a second mod (`autologin-gametest`) that boots a real client.
  It registers a real server `/login` command so the test asserts on the password
  the server received, not on what the client believes it sent.

`AutoLoginRuntime` is the seam between the two halves: `onServerText(String)` in,
`use(n)`/`send(account)` out. Keep it thin; rules belong in `core`.

## Rules that must not regress

These are the security properties, and each has a test:

- A password is only ever sent as a command, never as chat. Chat is broadcast to
  the whole server.
- The command verb comes from the message only when it is on the allowlist in
  `LoginVerbs` (`login`, `l`, `log`, `auth`, `authenticate`, `in`, `register`,
  `reg`, `signup`, `sign_up`). Anything else falls back to the rule's default.
- The mod never sends on its own. Detection results in a picker, always.
- Passwords never appear in chat history, in logs, or in command arguments. No
  `/autologin add <password>`.
- An unreadable vault is never overwritten. `VaultAccountStore` refuses to save
  while `problem()` is set, so a failed decrypt cannot wipe saved logins.

## Testing

```sh
./gradlew test                 # the unit suite, no client boot
./gradlew runClientGameTest    # real client + integrated server
```

The gametest needs a display. On a headless box:

```sh
Xvfb :99 -screen 0 1280x720x24 +extension GLX -noreset &
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClientGameTest
```

Screenshots: `build/run/clientGameTest/screenshots/000N_autologin-*.png`. Read
them; a green exit only proves nothing threw.

## Habits

Name loggers after the mod id, so log lines say which mod wrote them. Never log
a password: `LoginVerbs.describeCommand` masks the argument, use it.
