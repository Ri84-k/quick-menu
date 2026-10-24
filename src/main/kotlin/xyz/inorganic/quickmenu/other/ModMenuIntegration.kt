package xyz.inorganic.quickmenu.other

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
        return ConfigScreenFactory { parent ->
            createConfigScreen(parent)
        }
    }

    private fun createConfigScreen(parent: Screen?): Screen {
        val config = QuickMenu.CONFIG
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("text.config.quickmenu.title"))
            .save { config.save() }
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.menu"))
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.buttonsPerRow"))
                    .description(OptionDescription.of(Component.literal("How many Quick Menu action buttons fit across each row (1-15). This affects the G menu, not the radial wheel.")))
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(1)
                            .max(15)
                    }
                    .binding(5, { config.buttonsPerRow }, { config.buttonsPerRow = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.visibleRows"))
                    .description(OptionDescription.of(Component.literal("How many button rows are visible in Quick Menu before you need to scroll.")))
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(1)
                            .max(10)
                    }
                    .binding(2, { config.visibleRows }, { config.visibleRows = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.closeOnKeyReleased"))
                    .description(OptionDescription.of(Component.literal("Legacy option; the current G shortcut toggles the regular menu when pressed. This option has no separate effect in the current build.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.closeOnKeyReleased }, { config.closeOnKeyReleased = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.hideEditIcon"))
                    .description(OptionDescription.of(Component.literal("Hide the edit-pencil control in the regular G menu. You can still use its edit-mode shortcut.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.hideEditIcon }, { config.hideEditIcon = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.keepNavigationHistory"))
                    .description(OptionDescription.of(Component.literal("Remember the last folder you opened when returning to the G menu.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.keepNavigationHistory }, { config.keepNavigationHistory = it })
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.action_buttons"))
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.closeOnAction"))
                    .description(OptionDescription.of(Component.literal("Close the regular G menu after pressing an action button. Turn off to keep it open after actions.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.closeOnAction }, { config.closeOnAction = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.showActionsInTooltip"))
                    .description(OptionDescription.of(Component.literal("Show information about a button's actions when hovering over it in the regular menu.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.showActionsInTooltip }, { config.showActionsInTooltip = it })
                    .build())
                .option(Option.createBuilder<ModConfig.DisplayRunText>()
                    .name(Component.translatable("text.config.quickmenu.option.displayRunText"))
                    .description(OptionDescription.of(Component.literal("Choose when action execution messages are shown: always, only for keyboard-triggered actions, or never.")))
                    .controller { opt ->
                        EnumControllerBuilder.create(opt)
                            .enumClass(ModConfig.DisplayRunText::class.java)
                    }
                    .binding(ModConfig.DisplayRunText.KEYBIND_ONLY, { config.displayRunText }, { config.displayRunText = it })
                    .build())
                .option(Option.createBuilder<ModConfig.ImportMode>()
                    .name(Component.translatable("text.config.quickmenu.option.defaultImportMode"))
                    .description(OptionDescription.of(Component.literal("Choose how imported buttons are handled: ask, replace everything, merge by matching names, or add only.")))
                    .controller { opt ->
                        EnumControllerBuilder.create(opt)
                            .enumClass(ModConfig.ImportMode::class.java)
                    }
                    .binding(ModConfig.ImportMode.ASK, { config.defaultImportMode }, { config.defaultImportMode = it })
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.radial"))
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.radialMaxItems"))
                    .description(OptionDescription.of(Component.literal("Maximum slots on each wheel page, from 1 to 16. Eight is a comfortable starting point; more slots make buttons closer together.")))
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(1, 16)
                            .step(1)
                    }
                    .binding(8, { config.radialMaxItems }, { config.radialMaxItems = it })
                    .build())
                .option(Option.createBuilder<ModConfig.RadialDisplayMode>()
                    .name(Component.translatable("text.config.quickmenu.option.radialDisplayMode"))
                    .description(OptionDescription.of(Component.literal("STATIC keeps a fixed slot count; DYNAMIC uses exactly as many slots as the current category needs. Visibility is controlled separately by Show only Radial-selected actions.")))
                    .controller { opt ->
                        EnumControllerBuilder.create(opt)
                            .enumClass(ModConfig.RadialDisplayMode::class.java)
                    }
                    .binding(ModConfig.RadialDisplayMode.STATIC, { config.radialDisplayMode }, { config.radialDisplayMode = it })
                    .build())
                .option(Option.createBuilder<ModConfig.RadialActivationMode>()
                    .name(Component.literal("Radial activation mode"))
                    .description(OptionDescription.of(Component.literal("HOLD: hold R and release over an action to execute. TOGGLE: press R to open/close, left-click to run actions, press P for profiles, or Esc to cancel.")))
                    .controller { opt ->
                        EnumControllerBuilder.create(opt)
                            .enumClass(ModConfig.RadialActivationMode::class.java)
                    }
                    .binding(ModConfig.RadialActivationMode.HOLD, { config.radialActivationMode }, { config.radialActivationMode = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.radialRadius"))
                    .description(OptionDescription.of(Component.literal("Distance from the center to the wheel's buttons in screen pixels. A larger radius makes room for more actions and longer names.")))
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(30, 150)
                            .step(5)
                    }
                    .binding(60, { config.radialRadius }, { config.radialRadius = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.radialDeadZoneRadius"))
                    .description(OptionDescription.of(Component.literal("Neutral area around the center. Move the cursor here before releasing R to close the wheel without triggering an action.")))
                    .controller { opt ->
                        IntegerSliderControllerBuilder.create(opt)
                            .range(5, 50)
                            .step(5)
                    }
                    .binding(15, { config.radialDeadZoneRadius }, { config.radialDeadZoneRadius = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Group radial pages by mod"))
                    .description(OptionDescription.of(Component.literal("When enabled, group MaLiLib actions into separate pages for Litematica, Tweakeroo, MiniHUD and other mods. Mixed or non-MaLiLib buttons are shown under General. Scroll to switch pages.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialGroupByMod }, { config.radialGroupByMod = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Show only Radial-selected actions"))
                    .description(OptionDescription.of(Component.literal("Visibility is separate from Static/Dynamic layout. ON hides buttons not marked Radial in the action editor. OFF includes all buttons. Defaults OFF so existing Static layouts continue to show all actions. Enable to recreate the old Dynamic filtering.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(false, { config.radialOnlySelected }, { config.radialOnlySelected = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Mod-specific submenus"))
                    .description(OptionDescription.of(Component.literal("Show a top-level category wheel for mods and Favorites. Hold R and click a category to enter it; release R over an action to execute. Right-click to go back.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialModSubmenus }, { config.radialModSubmenus = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Favorites category"))
                    .description(OptionDescription.of(Component.literal("Show buttons marked Favorite in a dedicated category on the wheel. Set Favorite in the button editor.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialFavoritesPage }, { config.radialFavoritesPage = it })
                    .build())
                .option(Option.createBuilder<ModConfig.WheelProfile>()
                    .name(Component.literal("Active wheel profile"))
                    .description(OptionDescription.of(Component.literal("ALL shows every saved button. Building, Redstone, Farming and General show only buttons assigned to that profile. Press P while viewing the wheel to switch.")))
                    .controller { opt -> EnumControllerBuilder.create(opt).enumClass(ModConfig.WheelProfile::class.java) }
                    .binding(ModConfig.WheelProfile.ALL, { config.radialProfile }, { config.radialProfile = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Modern wheel appearance"))
                    .description(OptionDescription.of(Component.literal("Draw connected radial sectors with a highlighted selection and readable tooltips.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialModernLook }, { config.radialModernLook = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Use PNG radial textures"))
                    .description(OptionDescription.of(Component.literal("Use smooth transparent PNG wheel and highlighted sectors instead of the experimental pixelated geometry. Turn off to use the older renderer. Modern appearance must also be enabled.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialPngTextures }, { config.radialPngTextures = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Wheel action search"))
                    .description(OptionDescription.of(Component.literal("Press / while viewing the wheel to search all actions from the active profile. Search works even when the original hotkeys are unbound.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialSearchEnabled }, { config.radialSearchEnabled = it })
                    .build())
                .option(Option.createBuilder<Boolean>()
                    .name(Component.translatable("text.config.quickmenu.option.radialCloseOnAction"))
                    .description(OptionDescription.of(Component.literal("The current hold-R-to-select wheel always closes when R is released. This setting is retained for compatibility but does not change that behavior.")))
                    .controller { BooleanControllerBuilder.create(it) }
                    .binding(true, { config.radialCloseOnAction }, { config.radialCloseOnAction = it })
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("text.config.quickmenu.section.macro"))
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.macroMaxStepsPerTick"))
                    .description(OptionDescription.of(Component.literal("Safety limit for how many macro instructions can run in one game tick. Lower values reduce the chance of a runaway macro freezing the game.")))
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(100)
                            .max(1000000)
                    }
                    .binding(10000, { config.macroMaxStepsPerTick }, { config.macroMaxStepsPerTick = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.macroMaxRunSeconds"))
                    .description(OptionDescription.of(Component.literal("Safety timeout for how long a macro may run before it is stopped.")))
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(1)
                            .max(300)
                    }
                    .binding(10, { config.macroMaxRunSeconds }, { config.macroMaxRunSeconds = it })
                    .build())
                .option(Option.createBuilder<Int>()
                    .name(Component.translatable("text.config.quickmenu.option.macroMaxNesting"))
                    .description(OptionDescription.of(Component.literal("Maximum allowed nested loops, conditions, or macro calls. Lower limits help prevent unintended recursion.")))
                    .controller { opt ->
                        IntegerFieldControllerBuilder.create(opt)
                            .min(1)
                            .max(100)
                    }
                    .binding(16, { config.macroMaxNesting }, { config.macroMaxNesting = it })
                    .build())
                .build())
            .build()
            .generateScreen(parent)
    }
}