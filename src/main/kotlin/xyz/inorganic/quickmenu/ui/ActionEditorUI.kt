package xyz.inorganic.quickmenu.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.ui.components.ConfirmScreen
import xyz.inorganic.quickmenu.ui.components.QuickMenuButton
import xyz.inorganic.quickmenu.ui.popups.ItemPickerUI
import xyz.inorganic.quickmenu.ui.popups.CustomIconPickerUI

class ActionEditorUI(private val originalAction: ActionButtonData? = null) : Screen(Component.translatable("menu.editor.title")) {
    private var actionButtonData = ActionButtonData()
    private var isNewAction = true
    var previousScreen: Screen? = null

    private lateinit var nameEditBox: EditBox
    private lateinit var customModelDataEditBox: EditBox
    private lateinit var keybindBtn: Button
    private lateinit var folderCheckbox: Checkbox
    private lateinit var radialCheckbox: Checkbox
    private lateinit var favoriteCheckbox: Checkbox

    private var settingKeybind = false
    private var isBoundKeybind = false
    private var keybind = mutableListOf<Int>()

    private var menuX = 0
    private var menuY = 0
    private var menuWidth = 260
    private var menuHeight = 210

    init {
        originalAction?.let {
            actionButtonData = ActionButtonData(it.name, it.actions.toMutableList(), it.icon.copy(), it.keybind.toMutableList(), it.isFolder, it.children.toMutableList(), it.registeredForRadial, it.favorite, it.radialProfile, it.customIconName, it.radialOrder)
            keybind.addAll(it.keybind)
            if (keybind.size >= 4) isBoundKeybind = true
            isNewAction = false
        }
    }

    override fun init() {
        menuX = (width - menuWidth) / 2
        menuY = (height - (menuHeight + 40)) / 2

        nameEditBox = EditBox(font, menuX + 100, menuY + 15, 140, 20, Component.empty())
        nameEditBox.value = actionButtonData.name
        addRenderableWidget(nameEditBox)

        val iconButton = QuickMenuButton(actionButtonData.icon, {
            syncInputToData()
            val itemPicker = ItemPickerUI()
            itemPicker.previousScreen = this
            itemPicker.onSelectedItem = { item ->
                actionButtonData.icon = item
                actionButtonData.customIconName = ""
                rebuildWidgets()
            }
            minecraft?.gui?.setScreen(itemPicker)
        })
        iconButton.x = menuX + 100
        iconButton.y = menuY + 40
        addRenderableWidget(iconButton)

        customModelDataEditBox = EditBox(font, menuX + 100, menuY + 70, 140, 20, Component.empty())
        customModelDataEditBox.value = getCustomModelData(actionButtonData.icon)
        customModelDataEditBox.setResponder { updateCustomModelData() }
        addRenderableWidget(customModelDataEditBox)

        keybindBtn = Button.builder(Component.empty()) {
            syncInputToData()
            settingKeybind = true
            updateKeybindLabel()
        }.pos(menuX + 100, menuY + 95).size(140, 20).build()
        addRenderableWidget(keybindBtn)
        updateKeybindLabel()

        folderCheckbox = Checkbox.builder(Component.literal("Folder"), font)
            .pos(menuX + 10, menuY + 120)
            .selected(actionButtonData.isFolder)
            .onValueChange { _, value ->
                toggleFolderMode(value)
            }
            .build()
        addRenderableWidget(folderCheckbox)

        radialCheckbox = Checkbox.builder(Component.translatable("menu.editor.property.radial"), font)
            .pos(menuX + 100, menuY + 120)
            .selected(actionButtonData.registeredForRadial)
            .onValueChange { _, value ->
                actionButtonData.registeredForRadial = value
            }
            .build()
        addRenderableWidget(radialCheckbox)

        favoriteCheckbox = Checkbox.builder(Component.literal("Favorite"), font)
            .pos(menuX + 10, menuY + 145)
            .selected(actionButtonData.favorite)
            .onValueChange { _, value -> actionButtonData.favorite = value }
            .build()
        addRenderableWidget(favoriteCheckbox)

        // Profiles are labels on saved buttons, not duplicates of the actions.
        // ALL displays everything; individual profiles display matching labels.
        addRenderableWidget(Button.builder(Component.literal("Profile: ${actionButtonData.radialProfile}")) {
            syncInputToData()
            val profiles = listOf("GENERAL", "BUILDING", "REDSTONE", "FARMING")
            val index = profiles.indexOf(actionButtonData.radialProfile)
            actionButtonData.radialProfile = profiles[(index + 1).coerceAtLeast(0) % profiles.size]
            rebuildWidgets()
        }.pos(menuX + 100, menuY + 145).size(140, 20).build())

        addRenderableWidget(Button.builder(Component.literal(
            if (actionButtonData.customIconName.isBlank()) "Choose PNG..." else actionButtonData.customIconName.take(19))) {
            syncInputToData()
            val picker = CustomIconPickerUI()
            picker.previousScreen = this
            picker.onSelected = { filename ->
                actionButtonData.customIconName = filename
                rebuildWidgets()
            }
            minecraft?.gui?.setScreen(picker)
        }.pos(menuX + 100, menuY + 170).size(140, 20).build())

        if (!actionButtonData.isFolder) {
            addRenderableWidget(Button.builder(Component.literal("Edit Actions >")) {
                syncInputToData()
                val listEditor = ActionListEditorUI(actionButtonData)
                listEditor.previousScreen = this
                minecraft?.gui?.setScreen(listEditor)
            }.pos(menuX + 160, menuY + 120).size(80, 20).build())
        }

        val footerY = menuY + menuHeight + 5
        if (!isNewAction) {
            // Only saved buttons can be deleted. Unsaved buttons are discarded via Cancel.
            addRenderableWidget(Button.builder(Component.literal("Delete")) {
                confirmDelete()
            }.pos(menuX + 10, footerY).size(75, 20).build())
        }
        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.finish")) {
            saveAndClose()
        }.pos(menuX + if (isNewAction) 40 else 94, footerY).size(if (isNewAction) 80 else 72, 20).build())

        addRenderableWidget(Button.builder(Component.translatable("menu.editor.button.cancel")) {
            onClose()
        }.pos(menuX + if (isNewAction) 140 else 174, footerY).size(if (isNewAction) 80 else 75, 20).build())
    }

    private fun toggleFolderMode(isFolder: Boolean) {
        if (!isFolder && actionButtonData.children.isNotEmpty()) {
            val warningScreen = ConfirmScreen({ confirmed ->
                if (confirmed) {
                    actionButtonData.isFolder = false
                    rebuildWidgets()
                }
                minecraft?.gui?.setScreen(this)
            }, Component.literal("Convert to Action"), Component.literal("This folder has items. Move them to the parent list?"))
            minecraft?.gui?.setScreen(warningScreen)
        } else {
            actionButtonData.isFolder = isFolder
            rebuildWidgets()
        }
    }

    private fun syncInputToData() {
        if (::nameEditBox.isInitialized) actionButtonData.name = nameEditBox.value
        updateCustomModelData()
    }

    private fun updateKeybindLabel() {
        var msg = if (!isBoundKeybind) "Not Bound" else {
            if (keybind[3] == 0) {
                val base = InputConstants.Type.KEYSYM.getOrCreate(keybind[0]).displayName.string
                val mods = keybind[2]
                val sb = StringBuilder()
                if (mods and GLFW.GLFW_MOD_CONTROL != 0) sb.append("Ctrl+")
                if (mods and GLFW.GLFW_MOD_SHIFT != 0) sb.append("Shift+")
                if (mods and GLFW.GLFW_MOD_ALT != 0) sb.append("Alt+")
                sb.append(base)
                sb.toString()
            } else "Mouse ${keybind[0]}"
        }
        if (settingKeybind) msg = "> $msg <"
        keybindBtn.message = Component.literal(msg)
    }

    private fun getReadableKeyName(translationKey: String): String {
        return Component.translatable(translationKey).string
    }

    private fun getCustomModelData(item: ItemStack): String {
        if (item.isEmpty) return ""
        val cmd = item.get(DataComponents.CUSTOM_MODEL_DATA) ?: return ""
        return if (cmd.strings().isNotEmpty()) cmd.strings()[0] else ""
    }

    private fun updateCustomModelData() {
        if (!::customModelDataEditBox.isInitialized) return
        val text = customModelDataEditBox.value
        if (actionButtonData.icon.isEmpty) return
        try {
            if (text.isNotEmpty()) {
                val values = ActionButtonData.CustomModelDataValues(text)
                actionButtonData.icon.set(DataComponents.CUSTOM_MODEL_DATA, values.getComponent())
            } else {
                actionButtonData.icon.remove(DataComponents.CUSTOM_MODEL_DATA)
            }
        } catch (ignored: Exception) {}
    }

    override fun rebuildWidgets() {
        clearWidgets()
        init()
    }

    private fun confirmDelete() {
        val saved = originalAction ?: return
        val name = saved.name.ifBlank { "Unnamed action" }
        val message = if (saved.isFolder && saved.children.isNotEmpty()) {
            "Delete '$name' and all actions inside this folder?"
        } else {
            "Permanently delete '$name'?"
        }
        val dialog = ConfirmScreen({ confirmed ->
            if (confirmed) {
                // Search the full tree: the editor can also be opened from search results.
                fun deleteFrom(items: MutableList<ActionButtonData>): Boolean {
                    if (items.remove(saved)) return true
                    for (item in items) {
                        if (item.isFolder && deleteFrom(item.children)) return true
                    }
                    return false
                }
                if (deleteFrom(ActionButtonDataHandler.actions)) {
                    ActionButtonDataHandler.save()
                    minecraft?.gui?.setScreen(previousScreen)
                } else {
                    // If the action was moved or already removed, do not claim deletion.
                    minecraft?.gui?.setScreen(this)
                }
            } else {
                minecraft?.gui?.setScreen(this)
            }
        }, Component.literal("Confirm Delete"), Component.literal(message))
        minecraft?.gui?.setScreen(dialog)
    }

    private fun saveAndClose() {
        syncInputToData()
        actionButtonData.keybind = if (isBoundKeybind) keybind.toMutableList() else mutableListOf()

        val parentList = NavigationState.getCurrentChildren()

        if (isNewAction) {
            parentList.add(actionButtonData)
        } else {
            originalAction?.let {
                it.name = actionButtonData.name
                it.actions = actionButtonData.actions
                it.icon = actionButtonData.icon
                it.keybind = actionButtonData.keybind
                it.registeredForRadial = actionButtonData.registeredForRadial
                it.favorite = actionButtonData.favorite
                it.radialProfile = actionButtonData.radialProfile
                it.customIconName = actionButtonData.customIconName
                it.radialOrder = actionButtonData.radialOrder

                if (it.isFolder && !actionButtonData.isFolder && actionButtonData.children.isNotEmpty()) {
                    val index = parentList.indexOf(it)
                    if (index != -1) {
                        parentList.addAll(index + 1, actionButtonData.children)
                    } else {
                        parentList.addAll(actionButtonData.children)
                    }
                    actionButtonData.children.clear()
                }

                it.isFolder = actionButtonData.isFolder
                it.children = actionButtonData.children
            }
        }

        ActionButtonDataHandler.save()
        onClose()
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (settingKeybind) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                if (event.key() != GLFW.GLFW_KEY_LEFT_CONTROL && event.key() != GLFW.GLFW_KEY_RIGHT_CONTROL &&
                    event.key() != GLFW.GLFW_KEY_LEFT_SHIFT && event.key() != GLFW.GLFW_KEY_RIGHT_SHIFT &&
                    event.key() != GLFW.GLFW_KEY_LEFT_ALT && event.key() != GLFW.GLFW_KEY_RIGHT_ALT) {
                    isBoundKeybind = true
                    keybind.apply { clear(); add(event.key()); add(event.scancode()); add(event.modifiers()); add(0) }
                    settingKeybind = false
                    updateKeybindLabel()
                }
            } else {
                isBoundKeybind = false
                settingKeybind = false
                updateKeybindLabel()
            }
            return true
        }
        return super.keyPressed(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (settingKeybind) {
            if (event.button() <= 2) {
                isBoundKeybind = true
                keybind.apply { clear(); add(event.button()); add(0); add(0); add(1) }
            }
            settingKeybind = false
            updateKeybindLabel()
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val titleWidth = font.width(title)
        graphics.centeredText(font, title, (width - titleWidth) / 2, menuY - 15, -1)
        graphics.fill(menuX - 1, menuY - 1, menuX + menuWidth + 1, menuY + menuHeight + 1, 0x44000000.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC121212.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0x33FFFFFF.toInt())
        graphics.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())
        graphics.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0x33FFFFFF.toInt())
        graphics.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0x33FFFFFF.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.text(font, Component.translatable("menu.editor.property.name"), menuX + 10, menuY + 20, 0xFFAAAAAA.toInt(), true)
        graphics.text(font, Component.translatable("menu.editor.property.icon"), menuX + 10, menuY + 45, 0xFFAAAAAA.toInt(), true)
        graphics.text(font, "CustomModelData", menuX + 10, menuY + 75, 0xFFAAAAAA.toInt(), true)
        graphics.text(font, "PNG Icon", menuX + 10, menuY + 175, 0xFFAAAAAA.toInt(), true)
        graphics.text(font, Component.translatable("menu.editor.property.keybind"), menuX + 10, menuY + 100, 0xFFAAAAAA.toInt(), true)
    }

    override fun onClose() {
        minecraft?.gui?.setScreen(previousScreen) ?: super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}