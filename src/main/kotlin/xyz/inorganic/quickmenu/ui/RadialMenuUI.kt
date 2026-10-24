package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.other.ModConfig
import xyz.inorganic.quickmenu.other.MaLiLibHotkeys
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import kotlin.math.*

class RadialMenuUI : Screen(Component.translatable("menu.radial.title")) {
    private var centerX = 0
    private var centerY = 0
    private var currentPage = 0
    private var hoveredAction: ActionButtonData? = null
    private var hoveredSlotIndex = -1

    companion object {
        private var lastPage = 0
    }

    override fun init() {
        centerX = width / 2
        centerY = height / 2

        if (lastPage < totalPages()) {
            currentPage = lastPage
        } else {
            currentPage = 0
        }
    }

    private fun maxItemsPerPage(): Int {
        return QuickMenu.CONFIG.radialMaxItems.coerceIn(1, 16)
    }

    private fun collectRadialItems(items: List<ActionButtonData>, onlyRegistered: Boolean): List<ActionButtonData> {
        val result = mutableListOf<ActionButtonData>()
        for (item in items) {
            if (!item.isFolder) {
                if (!onlyRegistered || item.registeredForRadial) {
                    result.add(item)
                }
            } else {
                result.addAll(collectRadialItems(item.children, onlyRegistered))
            }
        }
        return result
    }

    private data class RadialPage(val group: String, val items: List<ActionButtonData>)

    /**
     * Partition the existing registered buttons without altering their saved order.
     * Grouping is calculated at render time, so old quickmenu_data.json remains valid.
     */
    private fun getPages(): List<RadialPage> {
        val config = QuickMenu.CONFIG
        val buttons = collectRadialItems(
            ActionButtonDataHandler.actions,
            config.radialDisplayMode == ModConfig.RadialDisplayMode.DYNAMIC
        )
        if (buttons.isEmpty()) return listOf(RadialPage("Empty", emptyList()))

        if (!config.radialGroupByMod) {
            return buttons.chunked(maxItemsPerPage()).map { RadialPage("All actions", it) }
        }

        val groups = linkedMapOf<String, MutableList<ActionButtonData>>()
        for (button in buttons) {
            val mods = button.actions.filterIsInstance<KeybindActionData>()
                .mapNotNull { MaLiLibHotkeys.getModName(it.translationKey) }
                .distinct()
            val group = if (mods.size == 1) mods.first() else "General"
            groups.getOrPut(group) { mutableListOf() }.add(button)
        }
        return groups.flatMap { (name, values) ->
            values.chunked(maxItemsPerPage()).map { RadialPage(name, it) }
        }
    }

    private fun getCurrentPage(): RadialPage {
        val pages = getPages()
        return pages[currentPage.coerceIn(0, pages.lastIndex)]
    }

    private fun getCurrentPageItems(): List<ActionButtonData> = getCurrentPage().items

    private fun totalPages(): Int = getPages().size

    private fun getSlotCount(): Int {
        return if (QuickMenu.CONFIG.radialDisplayMode == ModConfig.RadialDisplayMode.STATIC) {
            maxItemsPerPage()
        } else {
            getCurrentPageItems().size
        }
    }

    private fun toggleId(button: ActionButtonData?): String? {
        if (button == null || button.actions.size != 1) return null
        val key = button.actions[0] as? KeybindActionData ?: return null
        return key.translationKey.takeIf { MaLiLibHotkeys.isMaLiLib(it) }
    }

    /** Pixel-width-aware labels: no forced eight-character clipping. */
    private fun wrappedLabel(raw: String, maxPixels: Int, maxLines: Int): List<String> {
        val words = raw.replace('_', ' ').split(Regex("\\s+")).filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val combined = if (current.isEmpty()) word else "$"+"current $"+"word"
            if (font.width(combined) <= maxPixels) {
                current = combined
            } else {
                if (current.isNotEmpty()) {
                    lines.add(current)
                    current = ""
                }
                for (letter in word) {
                    if (font.width(current + letter) > maxPixels && current.isNotEmpty()) {
                        lines.add(current)
                        current = ""
                    }
                    current += letter
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        if (lines.size <= maxLines) return lines
        val visible = lines.take(maxLines).toMutableList()
        var last = visible.last()
        while (last.isNotEmpty() && font.width(last + "…") > maxPixels) {
            last = last.dropLast(1)
        }
        visible[visible.lastIndex] = last + "…"
        return visible
    }

    private fun getRadius(): Float {
        return QuickMenu.CONFIG.radialRadius.toFloat()
    }

    private fun getDeadZoneRadius(): Float {
        return QuickMenu.CONFIG.radialDeadZoneRadius.toFloat()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val total = totalPages()
        if (total <= 1) return false
        currentPage = ((currentPage - verticalAmount.toInt()) % total + total) % total
        lastPage = currentPage
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        updateHovered(mouseX, mouseY)
    }

    private fun updateHovered(mouseX: Double, mouseY: Double) {
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        val dist = sqrt(dx * dx + dy * dy).toFloat()

        if (dist < getDeadZoneRadius()) {
            hoveredAction = null
            hoveredSlotIndex = -1
            return
        }

        val items = getCurrentPageItems()
        val slots = getSlotCount()
        if (slots == 0) {
            hoveredAction = null
            hoveredSlotIndex = -1
            return
        }

        val sliceAngle = 2 * PI / slots
        val mouseAngle = atan2(dy, dx)
        val item0Center = -PI / 2 - sliceAngle / 2
        val shifted = (mouseAngle - item0Center + sliceAngle / 2 + 2 * PI) % (2 * PI)
        val slotIndex = (shifted / sliceAngle).toInt()

        hoveredSlotIndex = slotIndex
        hoveredAction = if (slotIndex in items.indices) items[slotIndex] else null
    }

    private fun isInDeadZone(mouseX: Double, mouseY: Double): Boolean {
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        return sqrt(dx * dx + dy * dy) <= getDeadZoneRadius()
    }

    fun handleKeyRelease() {
        val action = hoveredAction
        // Close before executing: a MaLiLib callback may open its own screen.
        minecraft?.gui?.setScreen(null)
        if (action != null && !action.isFolder) action.run()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        updateHovered(mouseX.toDouble(), mouseY.toDouble())

        graphics.fill(0, 0, width, height, 0xAA000000.toInt())

        val page = getCurrentPage()
        val items = page.items
        val slots = getSlotCount()
        val toggleIds = items.mapNotNull { toggleId(it) }.toSet()
        val states = MaLiLibHotkeys.getToggleStates(toggleIds)
        if (slots == 0) {
            val emptyMsg = Component.translatable("menu.main.no_actions")
            graphics.centeredText(font, emptyMsg, centerX, centerY, 0x66FFFFFF.toInt())
            return
        }

        val radius = getRadius()
        val deadZone = getDeadZoneRadius()
        val sliceAngle = (2 * PI / slots).toFloat()
        val startAngle = (-PI / 2 - sliceAngle / 2).toFloat()

        for (slot in 0 until slots) {
            val angle = (startAngle + slot * sliceAngle).toDouble()
            val itemX = (centerX + radius * cos(angle)).toInt()
            val itemY = (centerY + radius * sin(angle)).toInt()
            val hasItem = slot < items.size
            val item = if (hasItem) items[slot] else null
            val isHovered = slot == hoveredSlotIndex

            val toggle = states[toggleId(item)]
            val bgColor = when {
                isHovered -> 0x66FFFFFF.toInt()
                toggle == true -> 0x6646AA62
                toggle == false -> 0x33333333
                hasItem -> 0x44FFFFFF.toInt()
                else -> 0x22FFFFFF.toInt()
            }
            val borderColor = when {
                toggle == true -> 0xFF66DD77.toInt()
                toggle == false -> 0xFF888888.toInt()
                isHovered -> 0xAAFFFFFF.toInt()
                hasItem -> 0x22FFFFFF.toInt()
                else -> 0x11FFFFFF.toInt()
            }

            graphics.fill(itemX - 14, itemY - 14, itemX + 14, itemY + 14, bgColor)
            drawThinBorder(graphics, itemX - 14, itemY - 14, 28, 28, borderColor)

            if (hasItem && !item!!.icon.isEmpty) {
                graphics.item(item.icon, itemX - 8, itemY - 8)
            }
            if (toggle != null) {
                val status = if (toggle) "ON" else "OFF"
                graphics.text(font, status, itemX + 10, itemY - 21,
                    if (toggle) 0xFF66DD77.toInt() else 0xFFAAAAAA.toInt(), true)
            }

            if (hasItem) {
                val labelLines = wrappedLabel(item!!.name, 90, 3)
                for ((lineIndex, line) in labelLines.withIndex()) {
                    graphics.text(font, line, itemX - font.width(line) / 2,
                        itemY + 16 + lineIndex * 9, 0xFFCCCCCC.toInt(), true)
                }
            }
        }

        val deadColor = if (isInDeadZone(mouseX.toDouble(), mouseY.toDouble())) 0x33FFFFFF.toInt() else 0x22FFFFFF.toInt()
        graphics.fill(centerX - deadZone.toInt(), centerY - deadZone.toInt(), centerX + deadZone.toInt(), centerY + deadZone.toInt(), deadColor)
        drawThinBorder(graphics, centerX - deadZone.toInt(), centerY - deadZone.toInt(), (deadZone * 2).toInt(), (deadZone * 2).toInt(), 0x44FFFFFF.toInt())

        val total = totalPages()
        if (total > 1 || QuickMenu.CONFIG.radialGroupByMod) {
            val pageText = "${page.group}  •  Page ${currentPage + 1} / $total  •  Scroll to switch"
            graphics.text(font, pageText, centerX - font.width(pageText) / 2,
                centerY - radius.toInt() - 34, 0xFFDDDDDD.toInt(), true)
        }

        // The full selected action label is always visible, even when long.
        val selected = hoveredAction
        if (selected != null) {
            val lines = wrappedLabel(selected.name, (width - 40).coerceAtLeast(100), 5)
            val state = states[toggleId(selected)]
            for ((lineIndex, line) in lines.withIndex()) {
                graphics.text(font, line, centerX - font.width(line) / 2,
                    centerY + radius.toInt() + 45 + lineIndex * 10,
                    if (state == true) 0xFF77DD88.toInt() else -1, true)
            }
        }
    }

    private fun drawThinBorder(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + 1, color)
        graphics.fill(x, y + h - 1, x + w, y + h, color)
        graphics.fill(x, y + 1, x + 1, y + h - 1, color)
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color)
    }

    override fun isPauseScreen(): Boolean = false
}
