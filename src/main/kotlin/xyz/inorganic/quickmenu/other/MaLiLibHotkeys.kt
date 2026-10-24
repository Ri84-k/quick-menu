package xyz.inorganic.quickmenu.other

import net.fabricmc.loader.api.FabricLoader

/**
 * Optional integration with MaLiLib 0.29.x.
 * Actions are resolved by mod/category/config name, not by physical keycode.
 * The bridge uses reflection so Quick Menu still launches without MaLiLib installed.
 */
object MaLiLibHotkeys {
    private const val PREFIX = "malilib:"

    data class Entry(
        val id: String,
        val modName: String,
        val category: String,
        val displayName: String,
        val boundKeys: String
    )

    private data class Located(val entry: Entry, val keybind: Any)

    fun isMaLiLib(id: String): Boolean = id.startsWith(PREFIX)

    private fun scan(): List<Located> {
        if (!FabricLoader.getInstance().isModLoaded("malilib")) return emptyList()
        return try {
            val managerClass = Class.forName("fi.dy.masa.malilib.event.InputEventHandler")
            val manager = managerClass.getMethod("getKeybindManager").invoke(null)
            val categories = manager.javaClass.getMethod("getKeybindCategories")
                .invoke(manager) as? Iterable<*> ?: return emptyList()

            buildList {
                for (rawCategory in categories) {
                    val category = rawCategory ?: continue
                    val cls = category.javaClass
                    val modName = cls.getMethod("getModName").invoke(category) as? String ?: continue
                    val categoryName = cls.getMethod("getCategory").invoke(category) as? String ?: "Hotkeys"
                    val hotkeys = cls.getMethod("getHotkeys").invoke(category) as? Iterable<*> ?: continue

                    for (rawHotkey in hotkeys) {
                        val hotkey = rawHotkey ?: continue
                        val hotkeyClass = hotkey.javaClass
                        val configName = hotkeyClass.getMethod("getName").invoke(hotkey) as? String ?: continue
                        val translatedName = (hotkeyClass.getMethod("getTranslatedName")
                            .invoke(hotkey) as? String).orEmpty().ifBlank { configName }
                        val keybind = hotkeyClass.getMethod("getKeybind").invoke(hotkey) ?: continue
                        val bound = keybind.javaClass.getMethod("getKeysDisplayString")
                            .invoke(keybind) as? String ?: ""
                        val id = PREFIX + modName + "|" + categoryName + "|" + configName
                        add(Located(Entry(id, modName, categoryName, translatedName, bound), keybind))
                    }
                }
            }
        } catch (_: ReflectiveOperationException) {
            emptyList()
        } catch (_: LinkageError) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun getEntries(): List<Entry> = scan().map { it.entry }
        .distinctBy { it.id }
        .sortedWith(compareBy({ it.modName.lowercase() }, { it.displayName.lowercase() }))

    fun getDisplayName(id: String): String {
        if (!isMaLiLib(id)) return id
        val entry = getEntries().firstOrNull { it.id == id }
        return if (entry != null) entry.modName + ": " + entry.displayName
            else id.removePrefix(PREFIX).replace('|', ':')
    }

    /**
     * Invokes the registered callback. This is appropriate for single-click and toggle
     * hotkeys; it does not synthesize sustained physical keyboard/mouse input.
     */
    fun activate(id: String): Boolean {
        if (!isMaLiLib(id)) return false
        return try {
            val target = scan().firstOrNull { it.entry.id == id } ?: return false
            val callback = target.keybind.javaClass.getMethod("getCallback")
                .invoke(target.keybind) ?: return false
            val settings = target.keybind.javaClass.getMethod("getSettings")
                .invoke(target.keybind)
            val preference = settings.javaClass.getMethod("getActivateOn")
                .invoke(settings) as? Enum<*>
            val actionClass = Class.forName("fi.dy.masa.malilib.hotkeys.KeyAction")
            // For BOTH, fire PRESS only; double callbacks may toggle twice.
            val desired = if (preference?.name == "RELEASE") "RELEASE" else "PRESS"
            val event = actionClass.enumConstants.firstOrNull {
                (it as? Enum<*>)?.name == desired
            } ?: return false
            val callbackClass = Class.forName("fi.dy.masa.malilib.hotkeys.IHotkeyCallback")
            val keybindClass = Class.forName("fi.dy.masa.malilib.hotkeys.IKeybind")
            callbackClass.getMethod("onKeyAction", actionClass, keybindClass)
                .invoke(callback, event, target.keybind)
            true
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
