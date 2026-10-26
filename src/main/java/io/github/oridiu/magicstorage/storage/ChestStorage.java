package io.github.oridiu.magicstorage.storage;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

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
 * <p>
 * When two chests join into a large chest, their items move into the chest
 * under the large chest's main block, and the other position is recorded as
 * a link to it, so bags linked to either half open the large chest.
 */
public class ChestStorage {

    private final Path directory;
    private final Path linksFile;
    private final HytaleLogger logger;
    private final Map<ChestKey, SimpleItemContainer> containers = new ConcurrentHashMap<>();
    private final Set<ChestKey> dirty = ConcurrentHashMap.newKeySet();
    private final Map<ChestKey, CompletableFuture<Void>> pendingWrites = new ConcurrentHashMap<>();
    /** Delete still in flight for this chest. A newer delete bumps the number; only that delete clears it. */
    private final Map<ChestKey, Integer> deleting = new ConcurrentHashMap<>();
    /** Second half of a large chest -> its main block. */
    private final Map<ChestKey, ChestKey> links = new ConcurrentHashMap<>();
    private CompletableFuture<Void> pendingLinksWrite = CompletableFuture.completedFuture(null);

    public ChestStorage(@Nonnull Path directory, @Nonnull HytaleLogger logger) {
        this.directory = directory;
        this.linksFile = directory.resolveSibling("links.json");
        this.logger = logger;
        loadLinks();
    }

    /** The key the chest's items are stored under: the main block for either half of a large chest. */
    @Nonnull
    public ChestKey resolve(@Nonnull ChestKey key) {
        return links.getOrDefault(key, key);
    }

    /** The second half of a large chest, or null for a small chest. */
    @Nullable
    public ChestKey partnerOf(@Nonnull ChestKey origin) {
        for (Map.Entry<ChestKey, ChestKey> link : links.entrySet()) {
            if (link.getValue().equals(origin)) {
                return link.getKey();
            }
        }
        return null;
    }

    /**
     * Returns the chest's container, loading it from disk or creating an empty
     * one, and brings it to {@code capacity} slots: it grows if smaller, and
     * empty slots past that size are dropped, but never a slot holding items. Null when the save
     * file is there but cannot be read; that file is left as it is.
     */
    @Nullable
    public synchronized SimpleItemContainer getOrCreate(@Nonnull ChestKey key, short capacity) {
        ChestKey resolved = resolve(key);
        SimpleItemContainer container = containers.computeIfAbsent(resolved, k -> {
            if (!deleting.containsKey(k)) {
                SimpleItemContainer loaded = load(k);
                if (loaded != null) {
                    return watch(k, loaded);
                }
                if (Files.exists(file(k))) {
                    return null;
                }
            }
            dirty.add(k);
            return watch(k, new SimpleItemContainer(capacity));
        });
        if (container == null) {
            return null;
        }
        if (container.getCapacity() == capacity) {
            return container;
        }
        // Grow to the configured size, or drop empty slots past it. Slots that hold items stay where they are.
        short size = (short) Math.max(capacity, lastUsedSlot(container) + 1);
        if (size == container.getCapacity()) {
            return container;
        }
        SimpleItemContainer resized = new SimpleItemContainer(size);
        for (short slot = 0; slot < size && slot < container.getCapacity(); slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (!ItemStack.isEmpty(stack)) {
                resized.setItemStackForSlot(slot, stack, false);
            }
        }
        replace(resolved, container, resized);
        return resized;
    }

    /**
     * Joins two chests into one large chest stored under {@code origin}. Does
     * nothing if they are joined already. Bags linked to either half keep working.
     */
    @Nullable
    public synchronized SimpleItemContainer merge(@Nonnull ChestKey origin, @Nonnull ChestKey partner, short capacity) {
        if (origin.equals(links.get(partner))) {
            return getOrCreate(origin, capacity);
        }
        SimpleItemContainer main = get(origin);
        SimpleItemContainer other = get(partner);
        if ((main == null && Files.exists(file(resolve(origin)))) || (other == null && Files.exists(file(resolve(partner))))) {
            return null;
        }
        int used = countUsed(main) + countUsed(other);
        SimpleItemContainer merged = new SimpleItemContainer((short) Math.max(capacity, used));
        if (main != null) {
            main.moveAllItemStacksTo(merged);
        }
        if (other != null) {
            other.moveAllItemStacksTo(merged);
            containers.remove(partner, other);
            other.setGlobalFilter(FilterType.DENY_ALL);
            dirty.remove(partner);
            deleteFiles(partner);
        }
        if (main != null) {
            replace(origin, main, merged);
        } else {
            containers.put(origin, watch(origin, merged));
            dirty.add(origin);
        }
        // A large chest's partner could itself have been the main block of an older pair.
        links.replaceAll((from, to) -> to.equals(partner) ? origin : to);
        links.remove(origin);
        links.put(partner, origin);
        saveLinks();
        return merged;
    }

    /** Returns the chest's container, or null if no such chest exists (anymore). */
    @Nullable
    public synchronized SimpleItemContainer get(@Nonnull ChestKey requested) {
        ChestKey key = resolve(requested);
        SimpleItemContainer container = containers.get(key);
        if (container != null) {
            return container;
        }
        if (deleting.containsKey(key) || !Files.exists(file(key))) {
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
    public synchronized List<ItemStack> remove(@Nonnull ChestKey requested) {
        ChestKey key = resolve(requested);
        SimpleItemContainer container = get(key);
        containers.remove(key);
        dirty.remove(key);
        List<ItemStack> items = new ArrayList<>();
        if (container != null) {
            items.addAll(container.removeAllItemStacks());
            container.setGlobalFilter(FilterType.DENY_ALL);
        }
        deleteFiles(key);
        if (links.values().removeIf(key::equals)) {
            saveLinks();
        }
        return items;
    }

    /**
     * Writes every chest that changed since the last save. Called when a chest
     * window closes, when the world saves its chunks, and on shutdown.
     */
    public void saveDirty() {
        for (ChestKey key : List.copyOf(dirty)) {
            // Hold the lock through the queue so a delete cannot land ahead of this snapshot.
            synchronized (this) {
                if (!dirty.remove(key)) {
                    continue;
                }
                SimpleItemContainer container = containers.get(key);
                if (container == null) {
                    continue;
                }
                // Copy under the container's lock, then encode the snapshot.
                SimpleItemContainer snapshot = new SimpleItemContainer(container);
                BsonDocument document;
                try {
                    document = SimpleItemContainer.CODEC.encode(snapshot).asDocument();
                } catch (RuntimeException e) {
                    dirty.add(key);
                    logger.at(Level.SEVERE).withCause(e).log("[MagicStorage] Could not encode chest %s", key);
                    continue;
                }
                Path file = file(key);
                pendingWrites.compute(key, (k, previous) -> (previous != null ? previous : CompletableFuture.<Void>completedFuture(null))
                        .handle((result, error) -> null)
                        .thenCompose(ignored -> BsonUtil.writeDocument(file, document)));
            }
        }
    }

    /** Saves everything and waits until it is on disk. Called on shutdown. */
    public void saveAllAndWait() {
        dirty.addAll(containers.keySet());
        saveDirty();
        List<CompletableFuture<Void>> writes = new ArrayList<>(pendingWrites.values());
        writes.add(pendingLinksWrite);
        for (CompletableFuture<Void> write : writes) {
            try {
                write.join();
            } catch (RuntimeException e) {
                logger.at(Level.WARNING).withCause(e).log("[MagicStorage] Failed to save a chest");
            }
        }
    }

    /** Swaps in a new container; windows still open on the old one can't change it anymore. */
    private void replace(@Nonnull ChestKey key, @Nonnull SimpleItemContainer old, @Nonnull SimpleItemContainer replacement) {
        containers.put(key, watch(key, replacement));
        old.setGlobalFilter(FilterType.DENY_ALL);
        dirty.add(key);
    }

    /** Number of slots that hold items (not the number of items, which is what countItemStacks returns). */
    private static int countUsed(@Nullable SimpleItemContainer container) {
        if (container == null) {
            return 0;
        }
        int used = 0;
        for (short slot = 0; slot < container.getCapacity(); slot++) {
            if (!ItemStack.isEmpty(container.getItemStack(slot))) {
                used++;
            }
        }
        return used;
    }

    private static int lastUsedSlot(@Nonnull SimpleItemContainer container) {
        for (short slot = (short) (container.getCapacity() - 1); slot >= 0; slot--) {
            if (!ItemStack.isEmpty(container.getItemStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void deleteFiles(@Nonnull ChestKey key) {
        Path file = file(key);
        int generation = deleting.merge(key, 1, Integer::sum);
        pendingWrites.compute(key, (k, previous) -> (previous != null ? previous : CompletableFuture.<Void>completedFuture(null))
                .handle((result, error) -> {
                    deleteQuietly(file);
                    deleteQuietly(file.resolveSibling(file.getFileName() + ".bak"));
                    deleting.remove(key, generation);
                    return null;
                }));
    }

    private void loadLinks() {
        if (!Files.exists(linksFile)) {
            return;
        }
        try {
            BsonDocument document = BsonUtil.readDocumentNow(linksFile);
            if (document == null) {
                return;
            }
            for (BsonValue value : document.getArray("Links", new BsonArray())) {
                BsonDocument link = value.asDocument();
                links.put(ChestKey.CODEC.decode(link.get("From")), ChestKey.CODEC.decode(link.get("To")));
            }
        } catch (RuntimeException e) {
            logger.at(Level.SEVERE).withCause(e).log("[MagicStorage] Could not read %s", linksFile);
        }
    }

    private synchronized void saveLinks() {
        BsonArray array = new BsonArray();
        links.forEach((from, to) -> array.add(new BsonDocument()
                .append("From", ChestKey.CODEC.encode(from))
                .append("To", ChestKey.CODEC.encode(to))));
        BsonDocument document = new BsonDocument("Links", array);
        pendingLinksWrite = pendingLinksWrite
                .handle((result, error) -> null)
                .thenCompose(ignored -> BsonUtil.writeDocument(linksFile, document));
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
