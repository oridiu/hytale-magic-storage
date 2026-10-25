package io.github.oridiu.magicstorage;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.oridiu.magicstorage.command.MagicStorageCommand;
import io.github.oridiu.magicstorage.interaction.MagicBagInteraction;
import io.github.oridiu.magicstorage.interaction.MagicChestInteraction;
import io.github.oridiu.magicstorage.storage.ChestKey;
import io.github.oridiu.magicstorage.storage.ChestStorage;
import io.github.oridiu.magicstorage.system.ChestBreakSystem;
import io.github.oridiu.magicstorage.system.ChestPlaceSystem;
import io.github.oridiu.magicstorage.system.ChestSaveSystem;
import io.github.oridiu.magicstorage.system.KeepBagOnDeathSystems;
import io.github.oridiu.magicstorage.system.LinkedChestMarkerSystem;
import io.github.oridiu.magicstorage.system.OneBagSystems;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Entry point of the Magic Storage mod: a Magic Chest and a Magic Bag that
 * opens its linked chest from anywhere.
 */
public class MagicStoragePlugin extends JavaPlugin {

    private static MagicStoragePlugin instance;

    private final Config<MagicStorageConfig> config = withConfig(MagicStorageConfig.CODEC);
    private ChestStorage storage;

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
    }

    /** Reads config.json again, e.g. after an admin edited it by hand. */
    public CompletableFuture<MagicStorageConfig> reloadSettings() {
        return config.load();
    }

    @Override
    protected void setup() {
        Path chests = getDataDirectory().resolve("chests");
        ChestKey.setChestDirectory(chests);
        storage = new ChestStorage(chests, getLogger());

        getCodecRegistry(Interaction.CODEC).register(MagicChestInteraction.TYPE, MagicChestInteraction.class, MagicChestInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(MagicBagInteraction.TYPE, MagicBagInteraction.class, MagicBagInteraction.CODEC);

        getEntityStoreRegistry().registerSystem(new ChestBreakSystem());
        getEntityStoreRegistry().registerSystem(new ChestPlaceSystem());
        getEntityStoreRegistry().registerSystem(new LinkedChestMarkerSystem());
        getChunkStoreRegistry().registerSystem(new ChestSaveSystem());
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
        getLogger().at(Level.INFO).log("[MagicStorage] started, deposit via bag: %s", getSettings().isAllowDepositViaBag());
    }

    @Override
    public void shutdown() {
        if (storage != null) {
            storage.saveAllAndWait();
        }
        getLogger().at(Level.INFO).log("[MagicStorage] shutdown");
    }
}
