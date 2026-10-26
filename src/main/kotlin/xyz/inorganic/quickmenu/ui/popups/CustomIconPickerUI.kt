package xyz.inorganic.quickmenu.ui.popups

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.other.CustomIconTextures

/** Select PNG files the player copied into config/quickmenu/icons. */
class CustomIconPickerUI : Screen(Component.literal("Choose Custom PNG Icon")) {
    var previousScreen: Screen? = null
    var onSelected: (String) -> Unit = {}
    private var files: List<String> = emptyList()
    private var scroll = 0
    private var left = 0
    private var top = 0
    private var right = 0
    private var bottom = 0
    private var folderStatus = ""

    override fun init() {
        left = (width - 320) / 2
        right = left + 320
        top = (height - 260) / 2
        bottom = top + 260
        files = CustomIconTextures.listFiles()
        addRenderableWidget(Button.builder(Component.literal("Refresh")) {
            minecraft?.gui?.setScreen(this)
            rebuildWidgets()
        }.pos(left + 10, bottom - 27).size(75, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Open icons folder")) {
            try {
                val folder = CustomIconTextures.folder().toAbsolutePath().normalize()
                // Always provide a manual fallback: copy the exact instance-specific
                // path (important for Prism/Modrinth/multiple Minecraft profiles).
                minecraft?.keyboardHandler?.setClipboard(folder.toString())

                // Use Minecraft's own cross-platform launcher, the same approach
                // used for opening other game folders, instead of spawning a raw
                // explorer.exe process which can return without opening Explorer.
                try {
                    Util.getPlatform().openFile(folder.toFile())
                    folderStatus = "Folder requested; path copied (Ctrl+V in Explorer)"
                } catch (_: Exception) {
                    folderStatus = "Open failed; path copied (Ctrl+V in Explorer)"
                }
            } catch (_: Exception) {
                folderStatus = "Couldn't create icons folder (check config directory)"
            }
        }.pos(left + 90, bottom - 27).size(130, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Use Item Icon")) {
            onSelected("")
            onClose()
        }.pos(left + 225, bottom - 27).size(85, 20).build())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scroll = (scroll - (verticalAmount * 24).toInt()).coerceIn(0, maxOf(0, files.size * 24 - 162))
        return true
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (super.mouseClicked(event, doubled)) return true
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false
        if (event.x() < left + 10 || event.x() >= right - 10 ||
            event.y() < top + 57 || event.y() >= bottom - 39) return false
        val index = ((event.y() - (top + 57) + scroll) / 24).toInt()
        if (index in files.indices) {
            onSelected(files[index])
            onClose()
            return true
        }
        return false
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0x99000000.toInt())
        graphics.fill(left, top, right, bottom, 0xEE202028.toInt())
        graphics.text(font, "Custom PNG icons", left + 10, top + 10, -1, true)
        graphics.text(font, "Place PNGs in config/quickmenu/icons, then Refresh",
            left + 10, top + 25, 0xFFBBBBBB.toInt(), false)
        graphics.text(font, "Recommended: transparent 32x32 PNG (up to 512x512)",
            left + 10, top + 38, 0xFFAAAAAA.toInt(), false)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        graphics.enableScissor(left + 9, top + 57, right - 9, bottom - 39)
        var y = top + 57 - scroll
        for (name in files) {
            if (y + 24 > top + 57 && y < bottom - 39) {
                val hovered = mouseX in (left + 10) until (right - 10) && mouseY in y until (y + 24)
                graphics.fill(left + 10, y, right - 10, y + 23,
                    if (hovered) 0x667A9AC4 else 0x33484858)
                CustomIconTextures.draw(graphics, name, left + 14, y + 3, 18)
                graphics.text(font, Component.literal(name), left + 38, y + 7, -1, false)
            }
            y += 24
        }
        graphics.disableScissor()
        if (folderStatus.isNotEmpty()) {
            graphics.text(font, folderStatus, left + 11, bottom - 37, 0xFFADD4FF.toInt(), false)
        }
        if (files.isEmpty()) graphics.text(font, "No PNG files found. Use Open icons folder.",
            left + 12, top + 69, -1, false)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen)
    }
    override fun isPauseScreen() = false
}
