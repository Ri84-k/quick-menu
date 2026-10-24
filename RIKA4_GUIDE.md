# Quick Menu Rika.4 (Fabric 26.2)

Development branch: `feature/malilib-hotkey-wheel`. The upstream `main` remains untouched. This is a work-in-progress custom build of Quick Menu Renewed (GPL-3.0).

## Compatibility and persistence

- Existing `config/quickmenu_data.json` action names, icons, hotkeys, commands, MaLiLib identifiers, and buttons are preserved. Two optional per-button fields are appended (`favorite` and `radialProfile`) and are defaulted for legacy configurations.
- New preferences are in `config/quickmenu.json`: radial-only selection, mod submenus, favorites page, profiles, wheel styling, and search.
- Defaults deliberately leave **Show only Radial-selected actions** OFF. This keeps old Static installations showing their actions. To recreate the old Rika.3 Dynamic selection filtering, switch it ON.
- Before changing versions, back up both JSON files. Never install Rika.3 and Rika.4 together (same Fabric mod ID `quickmenu`).

## Quick start

- `G`: original action editor and button grid, unchanged.
- `R` (hold): open the wheel. With **Mod-specific submenus** enabled, it initially displays category buttons for installed mods and Favorites. **Hold R and left-click a category**, then move the cursor over an action and release R to execute. Alternatively, left-click an action to execute instantly.
- **Right-click** while viewing a category to return to the category wheel; **scroll** to change pages.
- Press `P` while the wheel is open to cycle profiles: ALL, BUILDING, REDSTONE, FARMING, GENERAL. The selection is saved.
- Press `/` or `F` while the wheel is open to launch searchable actions. Click a result to execute; Escape closes.
- In `G` > Edit > action editor, toggle **Favorite** and cycle **Profile** for each button. This does not change or rebind the underlying MaLiLib hotkey.
- In `G` Edit mode, hold **Alt** and drag a button onto another to reorder siblings; position saves immediately. Regular click/edit, Ctrl move, Shift delete remain available.

## Wheel settings

| Setting | Behavior |
|---|---|
| Display Mode Static | Fixed number of slots per page; may show empty slots |
| Display Mode Dynamic | Uses exactly as many slots as the current category has actions |
| Show only Radial-selected actions | Independently controls filtering for **both** display modes |
| Group radial pages by mod | Groups MaLiLib actions by supplying mod, with General for mixed actions |
| Mod-specific submenus | Adds click-to-open category wheel. Disable to restore flat pages |
| Favorites category | Displays starred actions in an additional category; originals remain in their mod category |
| Active wheel profile | ALL displays every action; other profiles show buttons tagged for the selected profile |
| Modern wheel appearance | Draws ring segments with hover and supported live ON/OFF states |
| Wheel action search | Allows `/` or `F` from the wheel |
| Max Items per Page | 1-16; 8 is recommended to prevent cramped segments |
| Radius and Dead Zone | Control ring spacing and neutral cursor radius |

## Live toggle indicators

- Uses live MaLiLib-backed boolean settings, not a click counter.
- Includes a guarded special-case reader for Litematica's `Configs.Generic.TOOL_ITEM_ENABLED` (ToolEnable).
- Not all callbacks provide a boolean state; those actions intentionally have no ON/OFF indicator.

## Known limitations / testing

- Profiles are **four named presets plus ALL**, not arbitrary user-created named profiles.
- The mod-specific submenu currently opens with **left click while R is held**; it is not automatically expanded on hover.
- Alt-drag reorders buttons **within the active Quick Menu folder**; it does not move items between folders.
- Rika.4 adds UI features and must be tested in Minecraft before being considered stable: category selection, favorite aliases, profiles, persistence after restart, search, toggle accuracy, and mouse interactions.
- Mod UI opening callbacks remain handled through MaLiLib's existing action bridge. The underlying hotkey need not be bound.

Build using JDK 25 and `./gradlew build` or GitHub Actions. Do not merge into `main` until user testing is completed.
