# Quick Menu Renewed + MaLiLib hotkey wheel (experimental)

This branch is an **independent modification** of [Quick Menu Renewed](https://github.com/tenkun0317/quick-menu) for Fabric Minecraft 26.2.

## Added

- A searchable **MaLiLib Hotkey** action picker (Litematica, Tweakeroo, MiniHUD, and any mod registered in MaLiLib's combined hotkey registry).
- MaLiLib actions can be assigned to regular Quick Menu buttons and then included in the built-in radial wheel.
- Existing Minecraft keybind picker, commands, macros, folders and action serialization are preserved.
- MaLiLib integration is **optional** at runtime (reflection bridge): the original controls continue to work without MaLiLib.
- Menu closing order is adjusted so shortcuts that open a different mod's configuration screen are not immediately closed by Quick Menu.

## In-game setup

1. Install only the custom Quick Menu JAR; **remove the upstream Quick Menu Renewed JAR**, since this fork retains the same Fabric mod ID.
2. Keep Fabric API, Fabric Language Kotlin, and your normal MaLiLib/Litematica/Tweakeroo/MiniHUD mods installed.
3. Open Quick Menu (default `G`) and turn on Edit mode.
4. Add an action button, then **+ Add Action > MaLiLib Hotkey**.
5. Search by mod or hotkey name and select an action.
6. To use the action in the radial wheel, mark the button for the wheel and hold the radial shortcut (default `R`).

## Limitations

- This first version directly invokes callbacks for **press/release-style actions**; it does not simulate held keys, continuous mouse movement, or arbitrary physical input.
- Some MaLiLib actions may require gameplay-specific state; test individual actions in a backup/testing world.
- Only hotkeys registered in MaLiLib's combined categories are listed.
- If a hotkey's internal mod/category/config identifier changes in a future mod update, the saved button may need to be re-selected.
- GUI features, older Minecraft variants, and other MaLiLib version combinations still require testing.

## Building and verification

The upstream project uses Stonecutter with Minecraft **26.2** as the active version. Use **JDK 25**:

```sh
./gradlew build
```

The GitHub Actions workflow in this branch was adjusted to use Java 25. The integration has been committed but **has not yet passed a complete Gradle build or in-game verification**.

Do not merge into `main` or distribute this experimental build before testing. Upstream code is GPL-3.0; preserve its LICENSE and attribution.
