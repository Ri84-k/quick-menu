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


## Rika custom radial wheel improvements (2.3.0-rika.2)

- **Full labels:** Each wheel item uses wrapped text instead of an eight-character cutoff. The complete selected button name appears below the wheel while hovered.
- **Grouped pages:** Existing saved buttons are automatically divided into pages by MaLiLib mod (Litematica, Tweakeroo, MiniHUD, etc.). Mixed/non-MaLiLib actions appear on a **General** page. Scroll the mouse wheel while holding `R` to switch pages. To recover the former flat order, disable **Group radial pages by mod** in Quick Menu settings.
- **Live toggle state:** Actions that contain exactly one MaLiLib boolean hotkey show **ON** or **OFF**, with a corresponding border color. Plain commands, held-key operations and hotkeys without a readable boolean value show **no state**, rather than a guessed value.
- **Automatic icons:** Newly selected MaLiLib actions with an empty button icon receive a suggested Minecraft item (pickaxe/tools, spyglass/camera, etc.). You can still customize the icon in the action editor using the existing item picker; icons you already chose are never replaced.
- **Better performance:** MaLiLib's hotkey registry is cached for up to one second, while boolean states are read live.

### Upgrading safely

1. Back up `config/quickmenu_data.json` and `config/quickmenu.json`.
2. Remove the previous custom Quick Menu JAR; the mod ID is still `quickmenu`, so do not install both versions.
3. Install the new `quickmenu-2.3.0-rika.2+26.2.jar`.
4. Open your existing wheel with `R`; no action migration should be necessary.
5. Verify a Litematica or Tweakeroo boolean toggle and a normal one-shot hotkey separately. Boolean indicators are only displayed for hotkeys exposing a MaLiLib boolean config.

The runtime changes require in-game validation; passing Gradle compilation is not a replacement for testing.
