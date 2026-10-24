# Quick Menu Rika.4.2 — installation and testing

This experimental build targets **Minecraft Fabric 26.2** on **Java 25**. Original upstream code and saved action records are preserved; only optional fields and UI features are added.

## Controls

Open **Mod Menu → Quick Menu → Radial Menu**:

- **Radial activation mode: HOLD** (default) — hold R, point to an action, release R to execute it. This preserves pre-4.2 behavior.
- **Radial activation mode: TOGGLE** — press R to open, press R again or Esc to close without executing, left-click to run an action. Left-click a category to enter, right-click to go back. P changes profiles while the menu remains open.
- **Modern wheel appearance** and **Use PNG radial textures** — enable both for the smooth radial PNG design. Turn PNG textures off to use the previous pixelated sector renderer; turn Modern off to use the older square-button style.

## Custom PNG icons

1. Place your PNGs into the **Minecraft instance** folder at `config/quickmenu/icons/`. In Prism Launcher or another profile-based launcher, this is within that instance's `.minecraft` directory.
2. Supported names contain letters, numbers, hyphens, underscores and a period before `.png`. Images must be **PNG**, at most **512×512**, and smaller than **2 MB**. Recommended: transparent **32×32** or **64×64** PNG.
3. Press **G → Edit → action → PNG Icon → Choose PNG...**, select a file and click **Finish**. If a file is added while the picker is open, click **Refresh**. **Copy folder path** places the local directory path on your clipboard.
4. The PNG is used in the G menu, radial wheel, and radial search. The original Minecraft item icon remains stored and serves as a fallback if the PNG is missing. **Use Item Icon** clears the PNG assignment.
5. Icons are loaded once per client session. After replacing the contents of an existing PNG file, restart Minecraft to see the updated art. PNG images themselves are local; sharing only the menu JSON does not transfer the PNG files.

**CustomModelData is different.** It changes Minecraft item models when an appropriate resource pack supports the item property; it is not the direct PNG icon picker.

## Deleting an action

Edit an existing saved action and choose **Delete**, then confirm. Unsaved new actions are discarded with Cancel. Deleting a folder also deletes its children, as described by the confirmation.

## Backward compatibility / data

- `config/quickmenu.json` retains existing keys and adds `radialActivationMode=HOLD` and `radialPngTextures=true` by default.
- `quickmenu_data.json` retains names, existing hotkey IDs, item icons, favorites, and profiles. The optional `customIconName` defaults to an empty string.
- This is a **development build**, so back up **both configuration JSON files** before installing; never run two Quick Menu jars with the same mod ID simultaneously.

## Suggested tests

1. Verify hold-R selection works with the new default.
2. Switch to Toggle, tap R to open and R again to close; Esc should also cancel without action execution.
3. Open the wheel, press P to change Building/Redstone/ALL without holding R.
4. Click a mod category, click an action to execute, right-click to return.
5. Select a 32×32 PNG from the icon picker and verify it renders in both menus; rename/remove the file and verify item fallback after restarting.
6. Try wheel pages containing 1, 4, 8, and 16 items; verify the highlighted wedge corresponds to the actual selected icon.
7. Restart Minecraft; verify saved hotkeys and custom icon filenames persist.
