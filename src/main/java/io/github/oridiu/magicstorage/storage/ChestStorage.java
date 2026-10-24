package io.github.oridiu.magicstorage.storage;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Holds the contents of all Magic Chests. Each chest is one
 * {@link SimpleItemContainer}, shared by the chest itself and every bag
 * linked to it, and saved as its own JSON file in the plugin's data folder.
 */
public class ChestStorage {

    private final Path directory;
    private final HytaleLogger logger;
    private final Map<ChestKey, SimpleItemContainer> containers = new ConcurrentHashMap<>();
    private final Set<ChestKey> dirty = ConcurrentHashMap.newKeySet();
    private final Map<ChestKey, CompletableFuture<Void>> pendingWrites = new ConcurrentHashMap<>();

    public ChestStorage(@Nonnull Path directory, @Nonnull HytaleLogger logger) {
        this.directory = directory;
        this.logger = logger;
    }

    /** Returns the chest's container, loading it from disk or creating an empty one. */
    @Nonnull
    public SimpleItemContainer getOrCreate(@Nonnull ChestKey key, short capacity) {
        return containers.computeIfAbsent(key, k -> {
            SimpleItemContainer loaded = load(k);
            if (loaded != null) {
                return watch(k, loaded);
            }
            dirty.add(k);
            return watch(k, new SimpleItemContainer(capacity));
        });
    }

    /** Returns the chest's container, or null if no such chest exists (anymore). */
    @Nullable
    public SimpleItemContainer get(@Nonnull ChestKey key) {
        SimpleItemContainer container = containers.get(key);
        if (container != null) {
            return container;
        }
        if (!Files.exists(file(key))) {
            return null;
        }
        return containers.computeIfAbsent(key, k -> {
            SimpleItemContainer loaded = load(k);
            return loaded != null ? watch(k, loaded) : null;
        });
    }

    /**
     * Removes the chest for good and returns the items it held, so the caller
     * can drop them. Bags still open on it can't add anything anymore.
     */
    @Nonnull
    public List<ItemStack> remove(@Nonnull ChestKey key) {
        SimpleItemContainer container = get(key);
        containers.remove(key);
        dirty.remove(key);
        List<ItemStack> items = new ArrayList<>();
        if (container != null) {
            items.addAll(container.removeAllItemStacks());
            container.setGlobalFilter(FilterType.DENY_ALL);
        }
        Path file = file(key);
        pendingWrites.compute(key, (k, previous) -> (previous != null ? previous : CompletableFuture.<Void>completedFuture(null))
                .handle((result, error) -> {
                    deleteQuietly(file);
                    deleteQuietly(file.resolveSibling(file.getFileName() + ".bak"));
                    return null;
                }));
        return items;
    }

    /** Writes every chest that changed since the last save. */
    public void saveDirty() {
        for (ChestKey key : List.copyOf(dirty)) {
            dirty.remove(key);
            SimpleItemContainer container = containers.get(key);
            if (container == null) {
                continue;
            }
            // Copy under the container's lock, then encode the snapshot.
            SimpleItemContainer snapshot = new SimpleItemContainer(container);
            BsonDocument document = SimpleItemContainer.CODEC.encode(snapshot).asDocument();
            Path file = file(key);
            pendingWrites.compute(key, (k, previous) -> (previous != null ? previous : CompletableFuture.<Void>completedFuture(null))
                    .handle((result, error) -> null)
                    .thenCompose(ignored -> BsonUtil.writeDocument(file, document)));
        }
    }

    /** Saves everything and waits until it is on disk. Called on shutdown. */
    public void saveAllAndWait() {
        dirty.addAll(containers.keySet());
        saveDirty();
        for (CompletableFuture<Void> write : pendingWrites.values()) {
            try {
                write.join();
            } catch (RuntimeException e) {
                logger.at(Level.WARNING).withCause(e).log("[MagicStorage] Failed to save a chest");
            }
        }
    }

    private SimpleItemContainer watch(@Nonnull ChestKey key, @Nonnull SimpleItemContainer container) {
        container.registerChangeEvent(event -> {
            if (containers.get(key) == container) {
                dirty.add(key);
            }
        });
        return container;
    }

    @Nullable
    private SimpleItemContainer load(@Nonnull ChestKey key) {
        Path file = file(key);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            BsonDocument document = BsonUtil.readDocumentNow(file);
            return document != null ? SimpleItemContainer.CODEC.decode(document) : null;
        } catch (RuntimeException e) {
            logger.at(Level.SEVERE).withCause(e).log("[MagicStorage] Could not read chest file %s", file);
            return null;
        }
    }

    private Path file(@Nonnull ChestKey key) {
        return directory.resolve(key.fileName());
    }

    private void deleteQuietly(@Nonnull Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.at(Level.WARNING).withCause(e).log("[MagicStorage] Could not delete %s", file);
        }
    }
}
