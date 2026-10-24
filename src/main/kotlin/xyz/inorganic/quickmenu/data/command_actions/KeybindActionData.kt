package xyz.inorganic.quickmenu.data.command_actions

import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.other.KeybindHandler
import xyz.inorganic.quickmenu.other.MaLiLibHotkeys

@Serializable
class KeybindActionData(
    val translationKey: String = "",
    val mode: KeyPressMode = KeyPressMode.TAP
) : ActionData() {
    override val type: String = "key"
    override val value: String = translationKey
    
    override val typeString: String = "KEY"
    override fun getDisplayString(): String =
        if (MaLiLibHotkeys.isMaLiLib(translationKey)) MaLiLibHotkeys.getDisplayName(translationKey)
        else Component.translatable(translationKey).string
    
    override fun run() {
        if (MaLiLibHotkeys.isMaLiLib(translationKey)) {
            MaLiLibHotkeys.activate(translationKey)
            return
        }
        when (mode) {
            KeyPressMode.TAP -> KeybindHandler.pressKey(translationKey)
            KeyPressMode.PRESS -> KeybindHandler.holdKey(translationKey)
            KeyPressMode.RELEASE -> KeybindHandler.releaseKey(translationKey)
        }
    }
}

@Serializable
enum class KeyPressMode(val id: String) {
    TAP("tap"),
    PRESS("press"),
    RELEASE("release");

    val displayName: String
        get() = when (this) {
            TAP -> "Tap"
            PRESS -> "Press"
            RELEASE -> "Release"
        }

    fun next(): KeyPressMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): KeyPressMode = entries.find { it.id == id } ?: TAP
    }
}
