package io.github.oridiu.magicstorage;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Entry point of the Magic Storage mod: a Magic Chest and a Magic Bag that
 * opens its linked chest from anywhere.
 */
public class MagicStoragePlugin extends JavaPlugin {

    private static MagicStoragePlugin instance;

    public MagicStoragePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static MagicStoragePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[MagicStorage] setup");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("[MagicStorage] started");
    }

    @Override
    public void shutdown() {
        getLogger().at(Level.INFO).log("[MagicStorage] shutdown");
    }
}
