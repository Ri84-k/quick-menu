package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.other.MaLiLibHotkeys
import java.util.function.Consumer

/** Search hotkeys registered by MaLiLib mods, such as Litematica, Tweakeroo and MiniHUD. */
class MaLiLibPickerUI : Screen(Component.literal("Select MaLiLib Hotkey")) {
    var onSelectedKeybind: Consumer<String> = Consumer {}
    var previousScreen: Screen? = null

    private lateinit var searchBox: EditBox
    private var searchText = ""
    private var shown: List<MaLiLibHotkeys.Entry> = emptyList()
    private var scroll = 0
    private var menuX = 0
    private var menuY = 0
    private var menuW = 0
    private var menuH = 0
    private var listTop = 0
    private var listBottom = 0
    private var listLeft = 0
    private var listRight = 0

    companion object {
        private const val ROW_H = 24
    }

    override fun init() {
        menuW = minOf(430, width - 16)
        menuH = minOf(440, height - 16)
        menuX = (width - menuW) / 2
        menuY = (height - menuH) / 2
        listLeft = menuX + 10
        listRight = menuX + menuW - 10
        listTop = menuY + 43
        listBottom = menuY + menuH - 13

        clearWidgets()
        searchBox = EditBox(font, listLeft, menuY + 20, listRight - listLeft, 18, Component.empty())
        searchBox.setMaxLength(80)
        searchBox.setHint(Component.literal("Search mod, category or hotkey"))
        searchBox.setValue(searchText)
        searchBox.setResponder {
            searchText = it
            rebuild()
        }
        addRenderableWidget(searchBox)
        setFocused(searchBox)
        rebuild()
    }

    private fun maxScroll() = maxOf(0, shown.size * ROW_H - (listBottom - listTop))

    private fun rebuild() {
        val query = searchText.trim()
        shown = MaLiLibHotkeys.getEntries().filter {
            query.isEmpty() || sequenceOf(it.modName, it.category, it.displayName, it.boundKeys)
                .any { part -> part.contains(query, ignoreCase = true) }
        }
        scroll = scroll.coerceIn(0, maxScroll())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scroll = (scroll - (verticalAmount * ROW_H).toInt()).coerceIn(0, maxScroll())
        return true
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (super.mouseClicked(event, doubled)) return true
        if (event.x() >= listLeft && event.x() <= listRight &&
            event.y() >= listTop && event.y() < listBottom) {
            val index = ((event.y() - listTop + scroll) / ROW_H).toInt()
            if (index in shown.indices) {
                onSelectedKeybind.accept(shown[index].id)
                onClose()
                return true
            }
        }
        return false
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x55000000)
        graphics.fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xEE171717.toInt())
        graphics.text(font, Component.literal("MaLiLib hotkeys"), listLeft, menuY + 6, -1, true)
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.enableScissor(listLeft, listTop, listRight, listBottom)
        var y = listTop - scroll
        for (entry in shown) {
            if (y + ROW_H >= listTop && y < listBottom) {
                val hovered = mouseX in listLeft..listRight && mouseY in y until (y + ROW_H)
                graphics.fill(listLeft, y + 1, listRight, y + ROW_H - 1,
                    if (hovered) 0x44FFFFFF else 0x22FFFFFF)
                graphics.text(font,
                    Component.literal(entry.modName + ": " + entry.displayName),
                    listLeft + 4, y + 3, -1, true)
                val details = entry.category + " | " + entry.boundKeys.ifBlank { "Unbound" }
                graphics.text(font, Component.literal(details),
                    listLeft + 4, y + 13, 0xFFAAAAAA.toInt(), false)
            }
            y += ROW_H
        }
        graphics.disableScissor()

        if (shown.isEmpty()) {
            graphics.text(font, Component.literal("No MaLiLib hotkeys found"),
                listLeft, listTop + 8, 0xFFAAAAAA.toInt(), true)
        }
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }

    override fun isPauseScreen(): Boolean = false
}
