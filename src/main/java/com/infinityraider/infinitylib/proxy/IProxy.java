package com.infinityraider.infinitylib.proxy;

import com.infinityraider.infinitylib.config.Config;
import com.infinityraider.infinitylib.crafting.FallbackIngredient;
import com.infinityraider.infinitylib.entity.AmbientSpawnHandler;
import com.infinityraider.infinitylib.modules.Module;
import com.infinityraider.infinitylib.proxy.base.IProxyBase;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.function.Function;

public interface IProxy extends IProxyBase<Config> {

    @Override
    default  Function<ForgeConfigSpec.Builder, Config> getConfigConstructor() {
        return Config.Common::new;
    }

    @Override
    default void registerEventHandlers() {
        Module.getActiveModules().forEach(module -> {
            module.getCommonEventHandlers().forEach(this::registerEventHandler);
        });
        this.registerEventHandler(AmbientSpawnHandler.getInstance());
    }

    @Override
    default void registerCapabilities() {
        Module.getActiveModules().forEach(module -> {
            module.getCapabilities().forEach(this::registerCapability);
        });
    }

    @Override
    default void activateRequiredModules() {}

    @Override
    default void onCommonSetupEvent(FMLCommonSetupEvent event) {
        Module.getActiveModules().forEach(Module::init);
        FallbackIngredient.registerSerializer();
    }
}
