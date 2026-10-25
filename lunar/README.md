# Raven XD for Lunar Client 1.8.9 (Meowtils)

Runs Raven XD on **Lunar Client 1.8.9** through the Meowtils Lunar agent, without Forge.
Ported from the Raven B4 Lunar port (`../agent`).

## Install

1. Set up Meowtils on Lunar with its installer (it gives you the `-javaagent:...lunar-agent.jar=...agent-mods.json` JVM argument).
2. Put `RavenXDLoader-1.0.meowtils` in `.minecraft/meowtils/extensions`. Raven XD itself is bundled inside it.
3. In game, open the Meowtils menu, find **Raven XD** under Extensions and press **Load Raven XD**.
4. Fully restart the game.

**Unload Raven XD** removes it from the agent again (it stays active until the next restart).
If you'd rather manage the jar yourself, a `RavenXD.jar` placed in `.minecraft/meowtils` is used when the extension has none bundled.

## Build

```sh
cd ..  && ./gradlew build   # RavenXD-3.1.jar, run Gradle on JDK 21
cd lunar && ./build.sh      # dist/RavenXD.jar + dist/RavenXDLoader-1.0.meowtils
```

Needs JDK 11+, python3, unzip and jar. The loader compiles against the Meowtils jar: copy it to
`libs/Meowtils-<version>.jar` or set `MEOWTILS_JAR`.

## How it works

- **`tools/Patch.java`** rewrites the Forge jar's bytecode: the Forge API moves to `openraven.ravenxd.forge` (small
  replacements in `src/shim`), and calls to methods Forge adds to vanilla classes go to `forge/Compat`.
- **`src/mixins`** fire the Forge events vanilla never fires (ticks, render, chat, GUI, input, world join...).
  `lunar/LunarBootstrap` replaces `ClickGuiMod.init`; it runs at the end of `Minecraft.startGame` and also
  registers the jar's assets (capes, N+ icons, waifus, motion blur shader) as a resource pack.
- **`tools/Verify.java`** fails the build if the result still needs Forge: every field and method reference must
  exist in this jar or in vanilla 1.8.9, and every mixin target must exist under MCP names (what Lunar runs with).
  Run it after changing Raven XD; anything it reports would only break in-game.
- **`loader/`** is the Meowtils extension. Load adds Raven XD to the agent's `agent-mods.json`; the agent then
  adds the jar to Lunar's class loader, remaps SRG to MCP names and applies `mixins.ravenxd.json` on the next launch.

## Status

Built and verified, not yet tested in-game on Lunar.
