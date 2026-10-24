package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.other.CustomIconTextures
import xyz.inorganic.quickmenu.ui.popups.ItemPickerUI
import xyz.inorganic.quickmenu.ui.popups.CustomIconPickerUI
import java.util.Locale
import java.util.function.Consumer

/** Change a mod-category icon without altering action icons or their saved order. */
class CategoryIconEditorUI(private val category: String) : Screen(Component.literal("Category Icon")) {
    var previousScreen: Screen? = null

    private fun key() = category.lowercase(Locale.ROOT)
    private fun current() = QuickMenu.CONFIG.radialCategoryIcons[key()].orEmpty()

    private fun save(value: String) {
        if (value.isBlank()) QuickMenu.CONFIG.radialCategoryIcons.remove(key())
        else QuickMenu.CONFIG.radialCategoryIcons[key()] = value
        QuickMenu.CONFIG.save()
    }

    override fun init() {
        val x = (width - 260) / 2
        val y = (height - 155) / 2
        addRenderableWidget(Button.builder(Component.literal("Choose Minecraft Item")) {
            val picker = ItemPickerUI()
            picker.previousScreen = this
            picker.onSelectedItem = Consumer { stack ->
                if (!stack.isEmpty) {
                    val id = BuiltInRegistries.ITEM.getKey(stack.item)
                    save("item:$id")
                }
            }
            minecraft?.gui?.setScreen(picker)
        }.pos(x + 10, y + 55).size(240, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Choose Custom PNG")) {
            val picker = CustomIconPickerUI()
            picker.previousScreen = this
            picker.onSelected = { name ->
                if (name.isBlank()) save("") else save("png:$name")
            }
            minecraft?.gui?.setScreen(picker)
        }.pos(x + 10, y + 80).size(240, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Restore Default")) {
            save("")
        }.pos(x + 10, y + 105).size(116, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Done")) {
            onClose()
        }.pos(x + 134, y + 105).size(116, 20).build())
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val x = (width - 260) / 2
        val y = (height - 155) / 2
        graphics.fill(0, 0, width, height, 0x88000000.toInt())
        graphics.fill(x, y, x + 260, y + 155, 0xEE1B202A.toInt())
        graphics.fill(x, y, x + 260, y + 2, 0xFF6499DC.toInt())
        graphics.centeredText(font, Component.literal("$category — Category Icon"), width / 2, y + 12, -1)
        val value = current()
        graphics.centeredText(font, Component.literal(
            if (value.isBlank()) "Using default mod icon" else value.take(40)
        ), width / 2, y + 34, 0xFFC4CBD8.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun onClose() { minecraft?.gui?.setScreen(previousScreen) }
    override fun isPauseScreen(): Boolean = false
}
