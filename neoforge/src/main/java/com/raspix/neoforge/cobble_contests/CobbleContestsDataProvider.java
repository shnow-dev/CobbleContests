package com.raspix.neoforge.cobble_contests;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.data.DataProvider;
import com.cobblemon.mod.common.api.data.DataRegistry;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
//import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.cobblemon.mod.common.util.DistributionUtilsKt.server;
import static com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource;
import static java.util.Collections.emptyList;

public class CobbleContestsDataProvider implements DataProvider {

    private boolean canReload = true;
    // Both Forge n Fabric keep insertion order so if a registry depends on another simply register it after
    private Set<DataRegistry> registries = new HashSet<>();
    private List<UUID> synchronizedPlayerIds = new LinkedList<>();

    private Map<UUID, List<Runnable>> scheduledActions = new HashMap<>();


    public void registerDefaults(){
        CobbleContestsForge.LOGGER.info("Registering Defaults");
        this.register(CobbleContestsMoves.INSTANCE, true);



        //Cobblemon.implementation.registerResourceReloader(cobblemonResource("client_resources"), new SimpleResourceReloader(PackType.CLIENT_RESOURCES), PackType.CLIENT_RESOURCES, emptyList());

        Cobblemon.implementation.registerResourceReloader(cobblemonResource("data_resources"), new SimpleResourceReloader(PackType.SERVER_DATA), PackType.SERVER_DATA, emptyList());
    }

    @Override
    public void doAfterSync(@NotNull ServerPlayer serverPlayer, @NotNull Function0<Unit> function0) {

    }

    @Nullable
    @Override
    public DataRegistry fromIdentifier(@NotNull ResourceLocation registryIdentifier) {
        return this.registries.stream()
                .filter(registry -> registry.getId().equals(registryIdentifier))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    @Override
    public <T extends DataRegistry> T register(@NotNull T registry, boolean T) {
        if (this.registries.isEmpty()) {
            //CobbleContestsForge.LOGGER.info("Note: Hello.");
        }
        this.registries.add(registry);
        CobbleContestsForge.LOGGER.info("Registered the {} registry", registry.getId().toString());
        CobbleContestsForge.LOGGER.debug("Registered the {} registry of class {}", registry.getId().toString(), registry.getClass().getCanonicalName());
        return registry;
    }

    @Override
    public void sync(@NotNull ServerPlayer serverPlayer) {

    }


    private class SimpleResourceReloader implements ResourceManagerReloadListener {
        PackType type;

        public SimpleResourceReloader(PackType type) {
            this.type = type;
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            // Check for a server running, this is due to the create a world screen triggering datapack reloads, these are fine to happen as many times as needed as players may be in the process of adding their datapacks.
            boolean isInGame = server() != null;
            if (isInGame && this.type == PackType.SERVER_DATA && !canReload) {
                return;
            }

            for (DataRegistry registry : registries) {
                if (registry.getType() == this.type) {
                    registry.reload(manager);
                }
            }

            if (isInGame && this.type ==  PackType.SERVER_DATA) {
                canReload = false;
            }
        }
    }
}
