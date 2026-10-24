package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import xyz.inorganic.quickmenu.other.MaLiLibHotkeys
import xyz.inorganic.quickmenu.other.CustomIconTextures

/**
 * Opens with / or F from the wheel. It is a standalone screen so releasing
 * the radial shortcut doesn't accidentally launch whichever item was hovered.
 */
class RadialSearchUI(private val available: List<ActionButtonData>) :
    Screen(Component.literal("Quick Menu • Search")) {

    private lateinit var search: EditBox
    private var textQuery = ""
    private var matches: List<ActionButtonData> = emptyList()
    private var scroll = 0
    private var left = 0
    private var right = 0
    private var top = 0
    private var bottom = 0
    private val rowHeight = 27

    private fun displayName(button: ActionButtonData): String =
        button.name.takeIf { it.isNotBlank() }
            ?: button.actions.firstOrNull()?.getDisplayString()?.takeIf { it.isNotBlank() }
            ?: "Unnamed action"

    override fun init() {
        left = width / 2 - minOf(245, (width - 30) / 2)
        right = width - left
        top = maxOf(30, height / 2 - 190)
        bottom = minOf(height - 30, top + 370)
        clearWidgets()
        search = EditBox(font, left + 12, top + 25, right - left - 24, 20, Component.empty())
        search.setMaxLength(100)
        search.setHint(Component.literal("Search actions in the current wheel profile"))
        search.setValue(textQuery)
        search.setResponder {
            textQuery = it
            scroll = 0
            refresh()
        }
        addRenderableWidget(search)
        setFocused(search)
        refresh()
    }

    private fun refresh() {
        val query = textQuery.trim()
        matches = available.filter { button ->
            query.isBlank() || displayName(button).contains(query, ignoreCase = true) ||
                button.actions.any { action ->
                    action.getDisplayString().contains(query, ignoreCase = true) ||
                        (action as? KeybindActionData)?.let {
                            MaLiLibHotkeys.getModName(it.translationKey)?.contains(query, ignoreCase = true)
                        } == true
                }
        }
        scroll = scroll.coerceIn(0, maxScroll())
    }

    private fun listTop() = top + 55
    private fun listBottom() = bottom - 18
    private fun maxScroll() = maxOf(0, matches.size * rowHeight - (listBottom() - listTop()))

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scroll = (scroll - (verticalAmount * rowHeight).toInt()).coerceIn(0, maxScroll())
        return true
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (super.mouseClicked(event, doubled)) return true
        if (event.x() in left.toDouble()..right.toDouble() &&
            event.y() >= listTop() && event.y() < listBottom()) {
            val index = ((event.y() - listTop() + scroll) / rowHeight).toInt()
            if (index in matches.indices) {
                val button = matches[index]
                minecraft?.gui?.setScreen(null)
                button.run()
                return true
            }
        }
        return false
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0xBB000000.toInt())
        graphics.fill(left, top, right, bottom, 0xFF202027.toInt())
        graphics.text(font, Component.literal("Find radial action"), left + 12, top + 8, -1, true)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        graphics.enableScissor(left + 4, listTop(), right - 4, listBottom())
        var y = listTop() - scroll
        for (button in matches) {
            if (y + rowHeight >= listTop() && y < listBottom()) {
                val hovered = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + rowHeight
                graphics.fill(left + 7, y + 1, right - 7, y + rowHeight - 2,
                    if (hovered) 0x88577799.toInt() else 0x44292933)
                if (!(button.customIconName.isNotEmpty() && CustomIconTextures.draw(graphics, button.customIconName, left + 12, y + 5, 16)) && !button.icon.isEmpty) graphics.item(button.icon, left + 12, y + 5)
                graphics.text(font, Component.literal(displayName(button).take(60)), left + 36, y + 7, -1, true)
                if (button.favorite) graphics.text(font, "★", right - 24, y + 7, 0xFFFFCC66.toInt(), true)
            }
            y += rowHeight
        }
        graphics.disableScissor()
        if (matches.isEmpty()) graphics.text(font, Component.literal("No matching actions"), left + 14, listTop() + 8, -1, true)
        graphics.text(font, Component.literal("${matches.size} actions • click to execute • Esc to close"),
            left + 10, bottom - 12, 0xFFAAAAAA.toInt(), true)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(null)
    }

    override fun isPauseScreen(): Boolean = false
}
