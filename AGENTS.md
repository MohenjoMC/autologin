# AGENTS.md

AutoLogin is a client-side Fabric mod that logs you in automatically on
servers that ask for it. Package is `com.monhenjo.autologin`. License is CC0.

## Two branches, one project

- `main` builds the 26.x line and above. Loom plugin `net.fabricmc.fabric-loom`,
  Java 25 bytecode.
- `1.21.11` builds the legacy line. Loom plugin `net.fabricmc.fabric-loom-remap`
  with `loom.officialMojangMappings()`, Java 21 bytecode.

The gradle files differ between branches. The source layout does not. When a
change applies to both lines, land it on both branches.

## Build

```sh
./gradlew build
```

Gradle must run on Java 25 on both branches, because Loom 1.19 refuses older
JVMs. On this box that is `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`. CI
does the same through setup-java. The mod still compiles to the right
bytecode, decided by `options.release` in build.gradle (25 on main, 21 on
1.21.11).

Jars land in `build/libs/` as `autologin-<mod_version>+<minecraft_version>.jar`.

## Version matrix

One source tree builds every version. CI (`.github/workflows/build.yml`)
passes `-P` overrides, and `gradle.properties` stays at one release:

```sh
./gradlew build -Pminecraft_version=26.3 -Pfabric_api_version=0.165.0+26.3 -Pminecraft_dependency="~26.3"
```

`minecraft_dependency` is expanded into the `depends.minecraft` field of
`fabric.mod.json` at build time, so a 26.1 jar says `~26.1`. Hardcoding a
Minecraft version into that field breaks the matrix.

To support a new release: read its values with `fabric versions <mc> --json`,
then add one matrix entry to `build.yml`.

## Where code goes

- `src/client` is where the work happens: entrypoint, mixins, login logic.
  The generated `ExampleClientMixin` is a placeholder.
- `src/main` holds shared code. `AutoLogin.java` carries `MOD_ID`, the logger,
  and the `id()` helper.
- `assets/icon.svg` at the root is the brand icon for the README. The icon the
  game loads is `src/main/resources/assets/autologin/icon.png`.

## Habits

Name loggers after the mod id, so log lines say which mod wrote them.

Keep pure logic (parsing, state handling) free of Minecraft classes so it can
run as a plain JUnit test. No tests exist yet; the first test in NoNick set
the pattern, copy it when login logic grows one.
