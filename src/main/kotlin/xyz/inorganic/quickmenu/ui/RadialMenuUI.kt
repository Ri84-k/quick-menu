package xyz.inorganic.quickmenu.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.core.registries.BuiltInRegistries
import java.util.Locale
import net.minecraft.network.chat.Component
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import xyz.inorganic.quickmenu.other.ActionButtonDataHandler
import xyz.inorganic.quickmenu.other.MaLiLibHotkeys
import xyz.inorganic.quickmenu.other.ModConfig
import xyz.inorganic.quickmenu.other.CustomIconTextures
import kotlin.math.*

/**
 * Rika.4 wheel: data always references original saved buttons, never clones them.
 * Root categories are opened with a left click while holding R; right click backs out.
 * The original release-R-to-execute behavior remains for action pages.
 */
class RadialMenuUI : Screen(Component.literal("Quick Menu Wheel")) {
    private data class Entry(val name: String, val button: ActionButtonData? = null, val category: String? = null)
    private data class Page(val title: String, val entries: List<Entry>)

    private var centerX = 0
    private var centerY = 0
    private var currentPage = 0
    private var activeCategory: String? = null
    private var hovered: Entry? = null
    private var hoveredIndex = -1
    private var draggingAction: ActionButtonData? = null
    private var dragDropTarget: ActionButtonData? = null
    private var blockedAfterNavigation = false
    private var blockedX = 0.0
    private var blockedY = 0.0

    companion object {
        private var lastPage = 0
    }

    override fun init() {
        centerX = width / 2
        centerY = height / 2
        currentPage = lastPage.coerceIn(0, pages().lastIndex)
    }

    private fun maxSlots(): Int = QuickMenu.CONFIG.radialMaxItems.coerceIn(1, 16)

    // Fallback labels are calculated, not saved, so existing action names and
    // Quick Menu JSON remain unchanged.
    private fun displayName(button: ActionButtonData): String =
        button.name.takeIf { it.isNotBlank() }
            ?: button.actions.firstOrNull()?.getDisplayString()?.takeIf { it.isNotBlank() }
            ?: "Unnamed action"

    private fun collect(buttons: List<ActionButtonData>, output: MutableList<ActionButtonData>) {
        for (button in buttons) {
            if (button.isFolder) collect(button.children, output)
            else output.add(button)
        }
    }

    private fun actions(): List<ActionButtonData> {
        val config = QuickMenu.CONFIG
        val all = mutableListOf<ActionButtonData>()
        collect(ActionButtonDataHandler.actions, all)
        return all.filter { button ->
            (!config.radialOnlySelected || button.registeredForRadial) &&
                (config.radialProfile == ModConfig.WheelProfile.ALL ||
                    button.radialProfile.equals(config.radialProfile.name, ignoreCase = true))
        }
    }

    private fun modName(button: ActionButtonData): String {
        val mods = button.actions.filterIsInstance<KeybindActionData>()
            .mapNotNull { MaLiLibHotkeys.getModName(it.translationKey) }.distinct()
        return if (mods.size == 1) mods.first() else "General"
    }

    private fun groups(): LinkedHashMap<String, List<ActionButtonData>> {
        val config = QuickMenu.CONFIG
        val all = actions()
        val result = linkedMapOf<String, List<ActionButtonData>>()
        if (config.radialFavoritesPage) {
            val favorite = all.filter { it.favorite }
            if (favorite.isNotEmpty()) result["Favorites"] = favorite
        }
        if (!config.radialGroupByMod) {
            result["All actions"] = all
        } else {
            for ((key, value) in all.groupBy { modName(it) }) {
                result[key] = value
            }
        }
        return LinkedHashMap(result.mapValues { (_, buttons) ->
            buttons.sortedWith(compareBy<ActionButtonData> { it.radialOrder })
        })
    }

    private fun pages(): List<Page> {
        val config = QuickMenu.CONFIG
        if (config.radialModSubmenus) {
            val grouped = groups()
            val category = activeCategory
            if (category == null) {
                return grouped.keys.map { Entry(it, category = it) }
                    .chunked(maxSlots()).map { Page("Categories • ${config.radialProfile.name}", it) }
                    .ifEmpty { listOf(Page("Categories • ${config.radialProfile.name}", emptyList())) }
            }
            val buttons = grouped[category] ?: emptyList()
            return buttons.map { Entry(displayName(it), button = it) }
                .chunked(maxSlots()).map { Page("$category • ${config.radialProfile.name}", it) }
                .ifEmpty { listOf(Page("$category", emptyList())) }
        }
        val buttons = actions()
        if (!config.radialGroupByMod) {
            return buttons.map { Entry(displayName(it), button = it) }
                .chunked(maxSlots()).map { Page("All actions • ${config.radialProfile.name}", it) }
                .ifEmpty { listOf(Page("All actions", emptyList())) }
        }
        val result = mutableListOf<Page>()
        val favorite = if (config.radialFavoritesPage) buttons.filter { it.favorite } else emptyList()
        if (favorite.isNotEmpty()) {
            result.addAll(favorite.map { Entry(displayName(it), button = it) }
                .chunked(maxSlots()).map { Page("Favorites", it) })
        }
        for ((group, items) in buttons.groupBy { modName(it) }) {
            result.addAll(items.map { Entry(displayName(it), button = it) }
                .chunked(maxSlots()).map { Page(group, it) })
        }
        return result.ifEmpty { listOf(Page("Empty", emptyList())) }
    }

    private fun page(): Page {
        val p = pages()
        currentPage = currentPage.coerceIn(0, p.lastIndex)
        return p[currentPage]
    }

    private fun count(page: Page): Int {
        // Root categories are navigation entries, not fixed action positions.
        // Distribute them around the full circle even in Static mode.
        if (QuickMenu.CONFIG.radialModSubmenus && activeCategory == null) {
            return maxOf(1, page.entries.size)
        }
        return if (QuickMenu.CONFIG.radialDisplayMode == ModConfig.RadialDisplayMode.STATIC)
            maxSlots() else maxOf(1, page.entries.size)
    }

    private fun mouseReset(x: Double, y: Double) {
        blockedAfterNavigation = true
        blockedX = x
        blockedY = y
        hovered = null
        hoveredIndex = -1
    }

    private fun updateHover(mouseX: Double, mouseY: Double) {
        if (blockedAfterNavigation) {
            if (hypot(mouseX - blockedX, mouseY - blockedY) < 14) {
                hovered = null
                hoveredIndex = -1
                return
            }
            blockedAfterNavigation = false
        }
        val radius = QuickMenu.CONFIG.radialDeadZoneRadius
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        if (hypot(dx, dy) < radius) {
            hovered = null
            hoveredIndex = -1
            return
        }
        val page = page()
        val slots = count(page)
        val sector = 2 * PI / slots
        val angle = atan2(dy, dx)
        val index = ((angle + PI / 2 + sector / 2 + 2 * PI) % (2 * PI) / sector).toInt()
        hoveredIndex = index
        hovered = page.entries.getOrNull(index)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        updateHover(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val total = pages().size
        if (total < 2) return true
        val step = if (verticalAmount > 0) -1 else if (verticalAmount < 0) 1 else 0
        currentPage = ((currentPage + step) % total + total) % total
        lastPage = currentPage
        mouseReset(mouseX, mouseY)
        return true
    }


    // Matches the familiar Alt + drag control used in the G menu.
    private fun isAltDown(): Boolean =
        InputConstants.isKeyDown(minecraft!!.window, GLFW.GLFW_KEY_LEFT_ALT) ||
            InputConstants.isKeyDown(minecraft!!.window, GLFW.GLFW_KEY_RIGHT_ALT)

    private fun canReorderCategory(): Boolean = activeCategory != null &&
        QuickMenu.CONFIG.radialModSubmenus &&
        activeCategory != "Favorites" && activeCategory != "All actions"

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (draggingAction != null) {
            updateHover(event.x(), event.y())
            dragDropTarget = hovered?.button
            return true
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val source = draggingAction
        if (source != null) {
            updateHover(event.x(), event.y())
            val target = hovered?.button ?: dragDropTarget
            draggingAction = null
            dragDropTarget = null
            if (target != null && source !== target && canReorderCategory()) {
                val ordered = groups()[activeCategory.orEmpty()].orEmpty().toMutableList()
                val from = ordered.indexOf(source)
                val to = ordered.indexOf(target)
                if (from >= 0 && to >= 0) {
                    val moving = ordered.removeAt(from)
                    ordered.add(to, moving)
                    ordered.forEachIndexed { index, button -> button.radialOrder = index }
                    ActionButtonDataHandler.save()
                }
            }
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && activeCategory == null) {
            updateHover(event.x(), event.y())
            val category = hovered?.category
            if (category != null) {
                val editor = CategoryIconEditorUI(category)
                editor.previousScreen = this
                minecraft?.gui?.setScreen(editor)
                return true
            }
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && canReorderCategory() && isAltDown()) {
            updateHover(event.x(), event.y())
            val button = hovered?.button
            if (button != null) {
                draggingAction = button
                dragDropTarget = button
                return true
            }
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && activeCategory != null) {
            activeCategory = null
            currentPage = 0
            lastPage = 0
            mouseReset(event.x(), event.y())
            return true
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateHover(event.x(), event.y())
            val selected = hovered
            if (selected?.category != null) {
                activeCategory = selected.category
                currentPage = 0
                lastPage = 0
                mouseReset(event.x(), event.y())
                return true
            }
            if (selected?.button != null) {
                minecraft?.gui?.setScreen(null)
                selected.button.run()
                return true
            }
            // Clicking the dead-zone returns to categories.
            if (activeCategory != null && hypot(event.x() - centerX, event.y() - centerY) < QuickMenu.CONFIG.radialDeadZoneRadius) {
                activeCategory = null
                currentPage = 0
                mouseReset(event.x(), event.y())
                return true
            }
        }
        return super.mouseClicked(event, doubled)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_P) {
            val values = ModConfig.WheelProfile.entries
            val config = QuickMenu.CONFIG
            config.radialProfile = values[(config.radialProfile.ordinal + 1) % values.size]
            config.save()
            activeCategory = null
            currentPage = 0
            mouseReset(centerX.toDouble(), centerY.toDouble())
            return true
        }
        if (QuickMenu.CONFIG.radialSearchEnabled &&
            (event.key() == GLFW.GLFW_KEY_SLASH || event.key() == GLFW.GLFW_KEY_F)) {
            minecraft?.gui?.setScreen(RadialSearchUI(actions()))
            return true
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && activeCategory != null) {
            activeCategory = null
            currentPage = 0
            mouseReset(centerX.toDouble(), centerY.toDouble())
            return true
        }
        return super.keyPressed(event)
    }

    fun handleKeyRelease() {
        val action = hovered?.button
        // Do not execute a category header. Only the explicit button action runs.
        minecraft?.gui?.setScreen(null)
        action?.run()
    }

    private fun iconFor(category: String) = when (category.lowercase()) {
        "favorites" -> net.minecraft.world.item.Items.NETHER_STAR.defaultInstance
        "litematica" -> net.minecraft.world.item.Items.GOLDEN_AXE.defaultInstance
        "tweakeroo" -> net.minecraft.world.item.Items.REPEATER.defaultInstance
        "tweakermore" -> net.minecraft.world.item.Items.COBWEB.defaultInstance
        "minihud" -> net.minecraft.world.item.Items.COMPASS.defaultInstance
        else -> net.minecraft.world.item.Items.BOOK.defaultInstance
    }

    private fun categoryOverride(category: String): String =
        QuickMenu.CONFIG.radialCategoryIcons[category.lowercase(Locale.ROOT)].orEmpty()

    private fun categoryItem(category: String): net.minecraft.world.item.ItemStack {
        val saved = categoryOverride(category)
        if (saved.startsWith("item:")) {
            val parts = saved.removePrefix("item:").split(":", limit = 2)
            if (parts.size == 2) {
                try {
                    return BuiltInRegistries.ITEM.getValue(
                        Identifier.fromNamespaceAndPath(parts[0], parts[1])
                    ).defaultInstance.copy()
                } catch (_: Exception) {
                    // Missing item: use the original mod-category icon.
                }
            }
        }
        return iconFor(category)
    }

    private fun toggleKey(button: ActionButtonData?): String? {
        if (button == null || button.actions.size != 1) return null
        val key = button.actions.first() as? KeybindActionData ?: return null
        return key.translationKey.takeIf { MaLiLibHotkeys.isMaLiLib(it) }
    }

    /** Draw small tiles along a circle rather than large overlapping square buttons. */
    private fun sector(graphics: GuiGraphicsExtractor, angle: Double, slice: Double, radius: Int, selected: Boolean, enabled: Boolean?, occupied: Boolean) {
        val color = when {
            !occupied -> 0x22383A48
            selected -> 0xAA7AB7FF.toInt()
            enabled == true -> 0xAA469A5A.toInt()
            enabled == false -> 0x995C5C5C.toInt()
            else -> 0x88505064.toInt()
        }
        val inner = maxOf(24, (radius * 0.51).toInt())
        val outer = radius + 26
        for (j in 0..9) {
            val a = angle - slice * 0.47 + j * slice * 0.94 / 9
            for (r in inner..outer step 7) {
                val x = (centerX + r * cos(a)).toInt()
                val y = (centerY + r * sin(a)).toInt()
                graphics.fill(x - 4, y - 4, x + 5, y + 5, color)
            }
        }
    }

    /**
     * A fixed transparent background PNG plus per-sector highlight PNG sprites.
     * The sprite rotates around the wheel's center, so 1..16 entries all align
     * with the actual mouse hit-testing angles.
     */
    private fun drawPngWheel(graphics: GuiGraphicsExtractor, slots: Int, radius: Int) {
        val diameter = (2 * (radius + 27)).coerceAtLeast(90)
        val left = centerX - diameter / 2
        val top = centerY - diameter / 2
        val background = Identifier.fromNamespaceAndPath("quickmenu", "textures/radial/ring.png")
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, left, top,
            0f, 0f, diameter, diameter, 256, 256, 256, 256)

        if (hoveredIndex in 0 until slots && hovered != null) {
            val wedge = Identifier.fromNamespaceAndPath("quickmenu", "textures/radial/wedge_$slots.png")
            val matrices = graphics.pose()
            matrices.pushMatrix()
            matrices.translate(centerX.toFloat(), centerY.toFloat())
            matrices.rotate((hoveredIndex * 2.0 * PI / slots).toFloat())
            matrices.translate(-centerX.toFloat(), -centerY.toFloat())
            graphics.blit(RenderPipelines.GUI_TEXTURED, wedge, left, top,
                0f, 0f, diameter, diameter, 256, 256, 256, 256)
            matrices.popMatrix()
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        updateHover(mouseX.toDouble(), mouseY.toDouble())
        graphics.fill(0, 0, width, height, 0x99000000.toInt())

        val p = page()
        val slots = count(p)
        val radius = QuickMenu.CONFIG.radialRadius
        val slice = 2 * PI / slots
        val states = MaLiLibHotkeys.getToggleStates(p.entries.mapNotNull { toggleKey(it.button) }.toSet())
        val modern = QuickMenu.CONFIG.radialModernLook
        val pngWheel = modern && QuickMenu.CONFIG.radialPngTextures
        if (p.entries.isEmpty()) {
            val config = QuickMenu.CONFIG
            val heading = when {
                config.radialProfile != ModConfig.WheelProfile.ALL ->
                    "No actions assigned to ${config.radialProfile.name}"
                config.radialOnlySelected -> "No radial-selected actions"
                else -> "No actions in Quick Menu"
            }
            graphics.centeredText(font, Component.literal("Wheel profile: ${config.radialProfile.name}"),
                centerX, centerY - 56, 0xFFEAEAEA.toInt())
            graphics.centeredText(font, Component.literal(heading), centerX, centerY - 15, -1)
            graphics.centeredText(font,
                Component.literal("Press P to switch profile; choose ALL to see everything"),
                centerX, centerY + 5, 0xFFAAAAAA.toInt())
            graphics.centeredText(font,
                Component.literal("Assign profiles in G > Edit > Action"),
                centerX, centerY + 20, 0xFFAAAAAA.toInt())
            return
        }

        if (pngWheel) drawPngWheel(graphics, slots, radius)

        for (i in 0 until slots) {
            val entry = p.entries.getOrNull(i)
            val angle = -PI / 2 + i * slice
            val xx = (centerX + radius * cos(angle)).toInt()
            val yy = (centerY + radius * sin(angle)).toInt()
            val selected = i == hoveredIndex && entry != null
            val enabled = states[toggleKey(entry?.button)]
            if (modern && !pngWheel) sector(graphics, angle, slice, radius, selected, enabled, entry != null)
            else if (!modern) {
                val color = if (selected) 0x885FA9FF.toInt() else 0x554A4A4A
                graphics.fill(xx - 22, yy - 22, xx + 22, yy + 22, color)
            }
            if (entry == null) continue
            val savedCategoryIcon = entry.category?.let { categoryOverride(it) }.orEmpty()
            val customName = entry.button?.customIconName.orEmpty().ifBlank {
                if (savedCategoryIcon.startsWith("png:")) savedCategoryIcon.removePrefix("png:") else ""
            }
            if (customName.isBlank() || !CustomIconTextures.draw(graphics, customName, xx - 9, yy - 9, 18)) {
                val icon = entry.button?.icon?.takeIf { !it.isEmpty }
                    ?: categoryItem(entry.category ?: "General")
                graphics.item(icon, xx - 8, yy - 8)
            }
            if (enabled != null) {
                graphics.text(font, if (enabled) "ON" else "OFF", xx + 13, yy - 22,
                    if (enabled) 0xFF7EE08D.toInt() else 0xFFB0B0B0.toInt(), true)
            }
            if (slots <= 5) {
                val clipped = if (font.width(entry.name) > 100) entry.name.take(13) + "…" else entry.name
                graphics.text(font, clipped, xx - font.width(clipped) / 2, yy + 20, 0xFFEAEAEA.toInt(), true)
            }
            if (entry.button?.favorite == true) {
                graphics.text(font, "★", xx - 19, yy - 17, 0xFFFFCC66.toInt(), true)
            }
        }
        // A readable center label and full tooltip replace overlapping captions.
        val toggleMode = QuickMenu.CONFIG.radialActivationMode == ModConfig.RadialActivationMode.TOGGLE
        val label = if (draggingAction != null) "REORDER"
            else if (activeCategory == null && QuickMenu.CONFIG.radialModSubmenus) "MODS"
            else if (toggleMode) "SELECT"
            else "RELEASE R"
        graphics.centeredText(font, Component.literal(label), centerX, centerY - 5, 0xFFC0C5D6.toInt())

        // Short two-line banner avoids overlapping category labels near the center.
        val totalPages = pages().size
        val bannerW = minOf(350, width - 12)
        val bannerX = centerX - bannerW / 2
        val bannerY = maxOf(4, centerY - radius - 103)
        graphics.fill(bannerX, bannerY, bannerX + bannerW, bannerY + 39, 0xE31E2635.toInt())
        graphics.fill(bannerX, bannerY, bannerX + bannerW, bannerY + 2, 0xFF6DA1EB.toInt())
        val title = if (activeCategory == null) "QUICK MENU  /  CATEGORIES"
            else "QUICK MENU  /  " + activeCategory!!.uppercase(Locale.ROOT)
        graphics.centeredText(font, Component.literal(title.take(44)), centerX, bannerY + 8, 0xFFF1F5FC.toInt())
        val subtitle = QuickMenu.CONFIG.radialProfile.name + " PROFILE    •    PAGE " +
            (currentPage + 1) + "/" + totalPages
        graphics.centeredText(font, Component.literal(subtitle), centerX, bannerY + 23, 0xFF9FC8FF.toInt())
        val hint = if (activeCategory == null)
            "Left-click: Open  •  Right-click: Edit icon  •  P: Profile"
        else "Alt + Drag: Reorder  •  Right-click: Back  •  Scroll: Pages"
        graphics.centeredText(font, Component.literal(hint), centerX,
            minOf(height - 13, centerY + radius + 96), 0xFFBFC6D5.toInt())

        val selection = hovered
        if (selection != null) {
            val action = selection.button
            val type = if (selection.category != null) "Open category: ${selection.category}" else
                "Action: ${selection.name} • ${action?.let { modName(it) } ?: "General"}"
            graphics.centeredText(font, Component.literal(type.take(100)), centerX,
                centerY + radius + 43, 0xFFFFFFFF.toInt())
            if (action != null) {
                val description = action.actions.joinToString(" + ") { it.getDisplayString() }.take(130)
                graphics.centeredText(font, Component.literal(description), centerX,
                    centerY + radius + 57, 0xFFAAAAAA.toInt())
                val state = states[toggleKey(action)]
                if (state != null) graphics.centeredText(font,
                    Component.literal(if (state) "State: ON" else "State: OFF"),
                    centerX, centerY + radius + 71,
                    if (state) 0xFF77DD88.toInt() else 0xFFAAAAAA.toInt())
            }
        }
    }

    override fun isPauseScreen(): Boolean = false
}
