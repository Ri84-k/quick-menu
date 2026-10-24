# Rika.4.3 — usability update (Minecraft 26.2)

This update builds on the tested Rika.4.2.1 Grid Placement hotfix and retains the same saved action format.

## MaLiLib hotkey picker
- A visible scrollbar now appears alongside the filtered hotkey list.
- Drag the scrollbar thumb to jump to distant entries; click the scrollbar track to jump instantly.
- The mouse wheel and search field still work normally.

## Local PNG icon picker
- **Open icons folder** opens `config/quickmenu/icons/` in the operating system's file manager.
- If opening the folder is unsupported, the path is copied to the clipboard instead.

## G menu
- **+ Action** and **Settings** are immediately visible above the normal Search and Edit row, even when edit mode is off.
- Use the pencil/Edit control to edit existing buttons. Import/Export remain in edit mode.

## Radial wheel
- **Alt + left mouse drag** reorders actions inside an open mod submenu, without changing the physical G menu button order.
- Drag onto a different action and release to save the new position. Current design supports moves within the current page/category, not drag between categories. Favorites and the combined All actions category are excluded to avoid ambiguous ordering.
- **Right-click a category on the root wheel** to open the new Category Icon screen. Choose a Minecraft item, custom local PNG, or restore the default icon. Normal **left-click** still enters that category.
- A clearer header separates the wheel title, profile and page number. Shorter center labels and a bottom hint bar reduce overlapping text.

## Recommended tests
1. Confirm Grid Placement still toggles on/off, including the Tweakeroo config value itself.
2. Scroll and drag the scrollbar in the MaLiLib picker. Search for a hotkey and verify the thumb updates.
3. Click Open icons folder; confirm Windows Explorer opens in the correct Minecraft instance.
4. Open G with edit mode off; verify + Action and Settings each work with one click. Edit an action via the pencil.
5. Toggle R wheel, enter Tweakeroo, hold Alt and drag an action to a different slot, release. Reopen the wheel and confirm its saved position. Check the G menu layout has not moved.
6. Right-click a category on the wheel's root, select a Minecraft item icon, return to the wheel. Repeat with a PNG and then reset. Restart Minecraft and confirm the chosen icon persists.
7. Verify Hold / Toggle R, existing keybind actions, saved PNG icons and previously configured profiles still work.

Keep only one Quick Menu JAR installed at a time and back up Quick Menu configuration before upgrading.
