package io.github.a5b84.darkloadingscreen.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuApiImpl implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
      return new ConfigScreenFactoryImpl();
    }
    return parent -> null;
  }
}
