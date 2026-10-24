package io.github.oridiu.magicstorage;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.oridiu.magicstorage.command.MagicStorageCommand;
import io.github.oridiu.magicstorage.interaction.MagicBagInteraction;
import io.github.oridiu.magicstorage.interaction.MagicChestInteraction;
import io.github.oridiu.magicstorage.storage.ChestStorage;
import io.github.oridiu.magicstorage.system.ChestBreakSystem;
import io.github.oridiu.magicstorage.system.ChestPlaceSystem;
import io.github.oridiu.magicstorage.system.KeepBagOnDeathSystems;
import io.github.oridiu.magicstorage.system.LinkedChestMarkerSystem;
import io.github.oridiu.magicstorage.system.OneBagSystems;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Entry point of the Magic Storage mod: a Magic Chest and a Magic Bag that
 * opens its linked chest from anywhere.
 */
public class MagicStoragePlugin extends JavaPlugin {

    private static MagicStoragePlugin instance;

    private final Config<MagicStorageConfig> config = withConfig(MagicStorageConfig.CODEC);
    private ChestStorage storage;
    private ScheduledFuture<?> autoSave;

    public MagicStoragePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static MagicStoragePlugin getInstance() {
        return instance;
    }

    public MagicStorageConfig getSettings() {
        return config.get();
    }

    public ChestStorage getStorage() {
        return storage;
    }

    /** Changes settings in game: applies them right away and writes config.json. */
    public synchronized void updateSettings(@Nonnull Consumer<MagicStorageConfig> change) {
        change.accept(getSettings());
        config.save();
        scheduleAutoSave();
    }

    /** Reads config.json again, e.g. after an admin edited it by hand. */
    public CompletableFuture<MagicStorageConfig> reloadSettings() {
        return config.load().thenApply(settings -> {
            scheduleAutoSave();
            return settings;
        });
    }

    private synchronized void scheduleAutoSave() {
        if (autoSave != null) {
            autoSave.cancel(false);
        }
        int interval = getSettings().getAutoSaveSeconds();
        autoSave = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(storage::saveDirty, interval, interval, TimeUnit.SECONDS);
    }

    @Override
    protected void setup() {
        storage = new ChestStorage(getDataDirectory().resolve("chests"), getLogger());

        getCodecRegistry(Interaction.CODEC).register(MagicChestInteraction.TYPE, MagicChestInteraction.class, MagicChestInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(MagicBagInteraction.TYPE, MagicBagInteraction.class, MagicBagInteraction.CODEC);

        getEntityStoreRegistry().registerSystem(new ChestBreakSystem());
        getEntityStoreRegistry().registerSystem(new ChestPlaceSystem());
        getEntityStoreRegistry().registerSystem(new LinkedChestMarkerSystem());
        getEntityStoreRegistry().registerSystem(new OneBagSystems.LimitInventory());
        getEntityStoreRegistry().registerSystem(new OneBagSystems.BlockCrafting());
        getEntityStoreRegistry().registerSystem(new KeepBagOnDeathSystems.Stash());
        getEntityStoreRegistry().registerSystem(new KeepBagOnDeathSystems.Restore());

        getCommandRegistry().registerCommand(new MagicStorageCommand());
    }

    @Override
    protected void start() {
        // Write the defaults once so server admins can find and edit the file.
        if (!Files.exists(getDataDirectory().resolve("config.json"))) {
            config.save();
        }
        scheduleAutoSave();
        getLogger().at(Level.INFO).log("[MagicStorage] started, deposit via bag: %s", getSettings().isAllowDepositViaBag());
    }

    @Override
    public void shutdown() {
        if (autoSave != null) {
            autoSave.cancel(false);
        }
        if (storage != null) {
            storage.saveAllAndWait();
        }
        getLogger().at(Level.INFO).log("[MagicStorage] shutdown");
    }
}
