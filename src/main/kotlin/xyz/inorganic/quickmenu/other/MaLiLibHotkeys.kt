package xyz.inorganic.quickmenu.other

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

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

    private data class Located(val entry: Entry, val keybind: Any, val hotkey: Any)

    private var cacheTimeNs: Long = 0L
    private var cachedEntries: List<Located> = emptyList()

    fun isMaLiLib(id: String): Boolean = id.startsWith(PREFIX)

    /** Stable, saved action IDs contain the supplying mod's display name. */
    fun getModName(id: String): String? {
        if (!isMaLiLib(id)) return null
        return id.removePrefix(PREFIX).substringBefore('|').ifBlank { null }
    }

    /** A default icon for newly created actions. Users can override it via the item picker. */
    fun suggestedIcon(id: String): ItemStack {
        val action = id.substringAfterLast('|').lowercase()
        val mod = getModName(id).orEmpty().lowercase()
        val item = when {
            "camera" in action || "freecam" in action -> Items.SPYGLASS
            "paste" in action || "schematic" in action -> Items.STRUCTURE_BLOCK
            "tool" in action || "selection" in action -> Items.GOLDEN_AXE
            "place" in action || "printer" in action -> Items.STICKY_PISTON
            "render" in action || "hud" in action -> Items.COMPASS
            "litematica" in mod -> Items.PAPER
            "tweakeroo" in mod -> Items.REPEATER
            "minihud" in mod -> Items.SPYGLASS
            else -> Items.REDSTONE_TORCH
        }
        return item.defaultInstance.copy()
    }

    /**
     * Read live boolean configs (including hotkeyed toggles). Plain action hotkeys
     * have no reliable on/off state and are omitted instead of assuming 'OFF'.
     */
    fun getToggleStates(ids: Set<String>): Map<String, Boolean> {
        if (ids.isEmpty()) return emptyMap()
        val states = mutableMapOf<String, Boolean>()
        for (located in scan()) {
            if (located.entry.id !in ids) continue
            try {
                val owner = located.hotkey
                val getter = owner.javaClass.getMethod("getBooleanValue")
                val state = getter.invoke(owner)
                if (state is Boolean) states[located.entry.id] = state
            } catch (_: ReflectiveOperationException) {
                // Hotkey isn't backed by a boolean config.
            } catch (_: SecurityException) {
                // No accessible state.
            }
        }
        return states
    }

    private fun scan(): List<Located> {
        if (!FabricLoader.getInstance().isModLoaded("malilib")) return emptyList()
        val now = System.nanoTime()
        if (cachedEntries.isNotEmpty() && now - cacheTimeNs < 1_000_000_000L) {
            return cachedEntries
        }
        return try {
            val managerClass = Class.forName("fi.dy.masa.malilib.event.InputEventHandler")
            val manager = managerClass.getMethod("getKeybindManager").invoke(null)
            val categories = manager.javaClass.getMethod("getKeybindCategories")
                .invoke(manager) as? Iterable<*> ?: return emptyList()

            val found = buildList {
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
                        add(Located(Entry(id, modName, categoryName, translatedName, bound), keybind, hotkey))
                    }
                }
            }
            cachedEntries = found
            cacheTimeNs = now
            found
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
