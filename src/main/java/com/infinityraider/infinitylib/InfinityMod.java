package com.infinityraider.infinitylib;

import com.google.common.collect.ImmutableList;
import com.infinityraider.infinitylib.config.ConfigurationHandler;
import com.infinityraider.infinitylib.network.INetworkWrapper;
import com.infinityraider.infinitylib.network.NetworkWrapper;
import com.infinityraider.infinitylib.proxy.base.IProxyBase;
import com.infinityraider.infinitylib.render.model.InfModelLoader;
import com.infinityraider.infinitylib.utility.InfinityLogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * This interface should be implemented in a mod's main class to have the registering of Items, Blocks, Renderers, etc. handled by InfinityLib
 * When implementing this interface, the mod must also be annotated with @InfinityMod
 */
public abstract class InfinityMod<P extends IProxyBase<C>, C extends ConfigurationHandler.SidedModConfig> {
    private final InfinityLogger logger;
    private final NetworkWrapper networkWrapper;
    private final P proxy;
    private final ConfigurationHandler<C> config;

    @SuppressWarnings("Unchecked")
    public InfinityMod() {
        //Populate static mod instance
        this.onModConstructed();
        //Create logger
        this.logger = new InfinityLogger(this);
        // Create network wrapper
        this.networkWrapper = new NetworkWrapper(this);
        // Create proxy
        this.proxy = this.createProxy();
        // Create configuration
        this.config = new ConfigurationHandler<>(ModLoadingContext.get(), this.proxy().getConfigConstructor());
        // Register FML mod loading cycle listeners
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::onCommonSetupEvent);
        bus.addListener(this::onClientSetupEvent);
        bus.addListener(this::onDedicatedServerSetupEvent);
        bus.addListener(this::onInterModEnqueueEvent);
        bus.addListener(this::onInterModProcessEvent);
        bus.addListener(this::onModLoadCompleteEvent);
        this.proxy().registerFMLEventHandlers(bus);
        MinecraftForge.EVENT_BUS.register(this);
        //Activate required modules
        this.proxy().activateRequiredModules();
        // Call for deferred, automatic registration of IInfinityRegistrable objects
        InfinityLib.instance.proxy().registerRegistrables(this);
        // Initialize the API
        this.initializeAPI();
    }

    private P createProxy() {
        P proxy = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> this::createClientProxy);
        if (proxy == null) {
            proxy = DistExecutor.unsafeCallWhenOn(Dist.DEDICATED_SERVER, () -> this::createServerProxy);
        }
        if (proxy == null) {
            // Can only happen if the mod fails to correctly implement the createClientProxy and/or the createServerProxy methods
            throw new RuntimeException("Failed to create SidedProxy for mod " + this.getModId() + " on side: " + FMLEnvironment.dist.name());
        }
        return proxy;
    }

    private void init() {
        // Register event handlers
        this.proxy().registerEventHandlers();
        // Register capabilities
        this.proxy().registerCapabilities();
        // Register messages
        this.networkWrapper.init();
    }

    private void initClient() {
        // Register renderers
        InfinityLib.instance.proxy().registerRenderers(this);
    }

    public final InfinityLogger getLogger() {
        return this.logger;
    }

    public final INetworkWrapper getNetworkWrapper() {
        return this.networkWrapper;
    }

    public P proxy() {
        return this.proxy;
    }

    public C getConfig() {
        return this.config.getConfig();
    }

    /**
     * @return The mod ID of the mod
     */
    public abstract String getModId();

    /**
     * Provides access to the instantiated mod object, for instance to store it in a static field
     */
    protected abstract void onModConstructed();

    /**
     * @return Creates the client proxy object for this mod
     */
    @OnlyIn(Dist.CLIENT)
    protected abstract P createClientProxy();

    /**
     * @return Creates the server proxy object for this mod
     */
    @OnlyIn(Dist.DEDICATED_SERVER)
    protected abstract P createServerProxy();

    /**
     * Register all messages added by this mod
     * @param wrapper NetworkWrapper instance to register messages to
     */
    public void registerMessages(INetworkWrapper wrapper) {}

    /**
     * Use to initialize the mod API
     */
    public void initializeAPI() {}


    /**
     * Used to register all of the mod's Blocks.
     * The object returned by this should have a field for each of its blocks
     * @return Block registry object or class
     */
    public Object getModBlockRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's TileEntities.
     * The object returned by this should have a field for each of its TileEntities
     * @return TileEntity registry object or class
     */
    public Object getModTileRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's Items.
     * The object returned by this should have a field for each of its items
     * @return Item registry object or class
     */
    public Object getModItemRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's Biomes.
     * The object returned by this should have a field for each of its biomes
     * @return Biome registry object or class
     */
    public Object getModBiomeRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's Enchantments.
     * The object returned by this should have a field for each of its enchantments
     * @return Enchantment registry object or class
     */
    public  Object getModEnchantmentRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's Entities.
     * The object returned by this should have a field for each of its entities
     * @return Entity registry object or class
     */
    public  Object getModEntityRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's Effects.
     * The object returned by this should have a field for each of its Potions
     * @return Potion registry object or class
     */
    public Object getModEffectRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's PotionTypes.
     * The object returned by this should have a field for each of its PotionTypes
     * @return PotionType registry object or class
     */
    public Object getModPotionTypeRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's SoundEvents.
     * The object returned by this should have a field for each of its SoundEvents
     * @return SoundEvent registry object or class
     */
    public Object getModSoundRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's ContainerTypes.
     * The object returned by this should have a field for each of its ContainerTypes
     * @return ContainerType registry object or class
     */
    public Object getModContainerRegistry() {
        return null;
    }

    /**
     * Used to register all of the mod's VillagerProfessions.
     * The object returned by this should have a field for each of its VillagerProfessions
     * @return VillagerProfession registry object or class
     */
    public Object getModVillagerProfessionRegistry() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public List<InfModelLoader<?>> getModModelLoaders() {
        return ImmutableList.of();
    }

    /**
     * --------------------------
     * FML Mod Loading Listeners
     * --------------------------
     */

    public final void onCommonSetupEvent(final FMLCommonSetupEvent event) {
        //self init
        this.init();
        //forward to proxy
        this.proxy().onCommonSetupEvent(event);
    }

    public final void onClientSetupEvent(final FMLClientSetupEvent event) {
        //self init
        this.initClient();
        //forward to proxy
        this.proxy().onClientSetupEvent(event);}

    public final void onDedicatedServerSetupEvent(final FMLDedicatedServerSetupEvent event) {
        //forward to proxy
        this.proxy().onDedicatedServerSetupEvent(event);
    }

    public final void onInterModEnqueueEvent(final InterModEnqueueEvent event) {
        //forward to proxy
        this.proxy().onInterModEnqueueEvent(event);
    }

    public final void onInterModProcessEvent(final InterModProcessEvent event) {
        //forward to proxy
        this.proxy().onInterModProcessEvent(event);
    }

    public final void onModLoadCompleteEvent(final FMLLoadCompleteEvent event) {
        //forward to proxy
        this.proxy().onModLoadCompleteEvent(event);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public final void onServerStartingEvent(final FMLServerStartingEvent event) {
        //forward to proxy
        this.proxy().onServerStartingEvent(event);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public final void onServerAboutToStartEvent(final FMLServerAboutToStartEvent event) {
        //forward to proxy
        this.proxy().onServerAboutToStartEvent(event);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public final void onServerStoppingEvent(final FMLServerStoppingEvent event) {
        //forward to proxy
        this.proxy().onServerStoppingEvent(event);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public final void onServerStoppedEvent(final FMLServerStoppedEvent event) {
        //forward to proxy
        this.proxy().onServerStoppedEvent(event);
    }

    /**
     * --------------------------
     * Proxy utility method calls
     * --------------------------
     */

    /**
     * @return The physical side, is always Side.SERVER on the server and Side.CLIENT on the client
     */
    public final Dist getPhysicalSide() {
        return this.proxy().getPhysicalSide();
    }

    /**
     * @return The effective side, on the server, this is always Side.SERVER, on the client it is dependent on the thread
     */
    public final LogicalSide getEffectiveSide() {
        return this.proxy().getLogicalSide();
    }

    /**
     * @return The minecraft server instance
     */
    public final MinecraftServer getMinecraftServer() {
        return this.proxy().getMinecraftServer();
    }

    /**
     * @return the instance of the EntityPlayer on the client, null on the server
     */
    public final PlayerEntity getClientPlayer() {
        return this.proxy().getClientPlayer();
    }

    /**
     * @return the client World object on the client, null on the server
     */
    public final World getClientWorld() {
        return this.proxy().getClientWorld();
    }

    /**
     * @return the client World object on the client, null on the server
     */
    public final World getWorldFromDimension(RegistryKey<World> dimension) {
        return this.proxy().getWorldFromDimension(dimension);
    }

    /**
     *  @return  the entity in that World object with that id
     */
    public final Entity getEntityById(World world, int id) {
        return this.proxy().getEntityById(world, id);
    }

    /**
     *  @return  the entity in that World object with that id
     */
    public final Entity getEntityById(RegistryKey<World> dimension, int id) {
        return this.proxy().getEntityById(dimension, id);
    }

    /** Queues a task to be executed on this side */
    public final void queueTask(Runnable task) {
        this.proxy().queueTask(task);
    }

    /** Registers an event handler */
    public final void registerEventHandler(Object handler) {
        this.proxy().registerEventHandler(handler);
    }
}
