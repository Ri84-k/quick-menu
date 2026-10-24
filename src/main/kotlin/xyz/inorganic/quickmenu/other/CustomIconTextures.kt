package xyz.inorganic.quickmenu.other

import com.mojang.blaze3d.platform.NativeImage
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Local icons are never embedded in quickmenu_data.json. Only a safe PNG filename is saved.
 * Missing or invalid files fall back to the original Minecraft item icon.
 */
object CustomIconTextures {
    data class ImageRef(val location: Identifier, val width: Int, val height: Int)
    private val textures = mutableMapOf<String, ImageRef>()
    private val rejected = mutableSetOf<String>()

    fun folder(): Path {
        val path = FabricLoader.getInstance().configDir.resolve("quickmenu").resolve("icons")
        Files.createDirectories(path)
        return path
    }

    private fun valid(name: String): Boolean =
        name.length in 5..90 && name.matches(Regex("[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.png", RegexOption.IGNORE_CASE)) &&
        !name.contains("..")

    fun listFiles(): List<String> = try {
        val root = folder()
        Files.list(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && valid(it.fileName.toString()) }
                .map { it.fileName.toString() }.sorted().toList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun texture(name: String): ImageRef? {
        if (!valid(name)) return null
        textures[name]?.let { return it }
        if (name in rejected) return null
        try {
            val path = folder().resolve(name)
            if (!Files.isRegularFile(path) || Files.size(path) > 2_000_000L) return null
            val image = Files.newInputStream(path).use { NativeImage.read(it) }
            if (image.width !in 1..512 || image.height !in 1..512) {
                image.close()
                rejected.add(name)
                return null
            }
            val clean = name.lowercase(Locale.ROOT).removeSuffix(".png")
                .replace(Regex("[^a-z0-9_-]"), "_")
            // Include case-sensitive hash to avoid collisions between similarly named files.
            val id = Identifier.fromNamespaceAndPath("quickmenu",
                "local_icons/${clean}_${name.hashCode().toUInt().toString(16)}")
            val dynamic = DynamicTexture({ "Quick Menu icon: $name" }, image)
            Minecraft.getInstance().textureManager.register(id, dynamic)
            val result = ImageRef(id, image.width, image.height)
            textures[name] = result
            return result
        } catch (_: Exception) {
            rejected.add(name)
            return null
        }
    }

    fun draw(graphics: GuiGraphicsExtractor, name: String, x: Int, y: Int, size: Int): Boolean {
        val image = texture(name) ?: return false
        graphics.blit(RenderPipelines.GUI_TEXTURED, image.location, x, y, 0f, 0f,
            size, size, image.width, image.height, image.width, image.height)
        return true
    }
}
