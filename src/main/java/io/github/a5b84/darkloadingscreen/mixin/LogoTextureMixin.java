package io.github.a5b84.darkloadingscreen.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.a5b84.darkloadingscreen.DarkLoadingScreen;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The vanilla {@code LoadingOverlay$LogoTexture#loadContents} method hardcodes the resource
 * provider to {@link Minecraft#getVanillaPackResources()}, which ignores all mod resources.
 *
 * <p>{@code LoadingOverlay.registerTextures()} runs before the initial resource reload, so
 * {@link Minecraft#getResourceManager()} is still empty when the logo's {@code loadContents} is
 * first invoked; vanilla would then render an ugly missing-texture (red/pink) fallback square
 * until the reload finishes. This mixin loads the logo from the mod's own bundled asset in that
 * case, so a mod-provided {@code assets/minecraft/textures/gui/title/mojangstudios.png} is
 * available right from the first frame.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.LoadingOverlay$LogoTexture")
public abstract class LogoTextureMixin {

  @Inject(method = "loadContents", at = @At("HEAD"), cancellable = true)
  private void loadCustomLogo(
      ResourceManager resourceManager, CallbackInfoReturnable<TextureContents> cir) {
    TextureContents contents = tryLoad(resourceManager);
    if (contents != null) {
      cir.setReturnValue(contents);
    }
  }

  private static TextureContents tryLoad(ResourceManager resourceManager) {
    // 1. The resource manager passed to this load call. During resource reloads it already
    //    contains all mods and resource packs.
    TextureContents contents = tryLoadFrom(resourceManager);
    if (contents != null) {
      return contents;
    }

    // 2. The full client resource manager, in case the reload hasn't been swapped in yet.
    contents = tryLoadFrom(Minecraft.getInstance().getResourceManager());
    if (contents != null) {
      return contents;
    }

    // 3. The logo bundled in the mod's own JAR, available even before the resource reload
    //    starts (this is what the user customizes to replace the Mojang logo).
    try (InputStream stream =
        DarkLoadingScreen.class.getResourceAsStream(
            "/assets/minecraft/textures/gui/title/mojangstudios.png")) {
      if (stream != null) {
        return new TextureContents(
            NativeImage.read(stream), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
      }
    } catch (IOException ignored) {
    }

    // 4. Let the vanilla implementation run (vanilla pack logo) as a last resort.
    return null;
  }

  private static TextureContents tryLoadFrom(ResourceManager resourceManager) {
    try (InputStream stream =
        resourceManager.open(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION)) {
      return new TextureContents(
          NativeImage.read(stream), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
    } catch (IOException ignored) {
      return null;
    }
  }
}