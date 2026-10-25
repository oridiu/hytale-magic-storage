package io.github.oridiu.magicstorage.storage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Identifies a Magic Chest by the world it stands in and its block position.
 * A Magic Bag stores this key in its item metadata.
 */
public final class ChestKey {

    public static final BuilderCodec<ChestKey> CODEC = BuilderCodec.builder(ChestKey.class, ChestKey::new)
            .append(new KeyedCodec<>("World", Codec.STRING), (key, value) -> key.world = value, key -> key.world)
            .add()
            .append(new KeyedCodec<>("X", Codec.INTEGER), (key, value) -> key.x = value, key -> key.x)
            .add()
            .append(new KeyedCodec<>("Y", Codec.INTEGER), (key, value) -> key.y = value, key -> key.y)
            .add()
            .append(new KeyedCodec<>("Z", Codec.INTEGER), (key, value) -> key.z = value, key -> key.z)
            .add()
            .build();

    /** Item metadata entry of a linked Magic Bag. */
    public static final KeyedCodec<ChestKey> BAG_LINK = new KeyedCodec<>("MagicStorageLink", CODEC);

    private static volatile Path chestDirectory;

    private String world;
    private int x;
    private int y;
    private int z;

    private ChestKey() {
    }

    public ChestKey(@Nonnull String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ChestKey of(@Nonnull String world, @Nonnull Vector3i pos) {
        return new ChestKey(world, pos.x, pos.y, pos.z);
    }

    /** Directory chest files are saved in, so a legacy file can be moved to its new name. */
    public static void setChestDirectory(@Nonnull Path directory) {
        chestDirectory = directory;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /**
     * Save file name. The world is lowercased, because the server stores worlds
     * that way, and any other unsafe character is percent-encoded.
     */
    public String fileName() {
        String distinct = encodedWorld(world) + "_" + x + "_" + y + "_" + z + ".json";
        Path directory = chestDirectory;
        if (directory == null || world == null) {
            return distinct;
        }
        String legacy = sanitized(world) + "_" + x + "_" + y + "_" + z + ".json";
        if (legacy.equals(distinct)) {
            return distinct;
        }
        synchronized (ChestKey.class) {
            Path from = directory.resolve(legacy);
            Path to = directory.resolve(distinct);
            if (Files.exists(to) || !Files.exists(from)) {
                return distinct;
            }
            // Old files turned every other character into '_'. Move one only when a single world could have written it.
            Boolean onlyThisWorld = legacyOwnedOnlyByThisWorld();
            if (onlyThisWorld == null) {
                return legacy;
            }
            if (!onlyThisWorld) {
                return distinct;
            }
            try {
                Files.move(from, to);
            } catch (IOException e) {
                return legacy;
            }
            return distinct;
        }
    }

    /** Letters, digits, '_' and '-' stay. Everything else is %xxxx so it cannot collapse into '_'. */
    private static String encodedWorld(String world) {
        String lower = world.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                out.append(c);
            } else {
                out.append('%');
                out.append(Integer.toHexString(c | 0x10000).substring(1));
            }
        }
        return out.toString();
    }

    private static String sanitized(String world) {
        return world.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    /** True when only this world maps to the old file name, false when another world does, null when that cannot be known. */
    @Nullable
    private Boolean legacyOwnedOnlyByThisWorld() {
        Universe universe;
        try {
            universe = Universe.get();
        } catch (RuntimeException e) {
            return null;
        }
        if (universe == null) {
            return null;
        }
        Set<String> owners = new HashSet<>();
        Map<String, World> loaded = universe.getWorlds();
        if (loaded != null) {
            for (World loadedWorld : loaded.values()) {
                if (loadedWorld != null) {
                    addOwner(owners, loadedWorld.getName());
                }
            }
        }
        Path worldsPath = universe.getWorldsPath();
        if (worldsPath == null || !Files.isDirectory(worldsPath)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldsPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    addOwner(owners, path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            return null;
        }
        if (owners.isEmpty()) {
            return true;
        }
        return owners.size() == 1 && owners.contains(world.toLowerCase(Locale.ROOT));
    }

    private void addOwner(Set<String> owners, @Nullable String name) {
        if (name != null && sanitized(name).equalsIgnoreCase(sanitized(world))) {
            owners.add(name.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChestKey other && x == other.x && y == other.y && z == other.z && world.equals(other.world);
    }

    @Override
    public int hashCode() {
        return ((world.hashCode() * 31 + x) * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return world + " " + x + " " + y + " " + z;
    }
}
