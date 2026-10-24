package io.github.oridiu.magicstorage.storage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
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

    /** File name of the chest's save file, safe on Windows and Linux. */
    public String fileName() {
        String name = encodeWorld(world) + "_" + x + "_" + y + "_" + z + ".json";
        moveLegacyFileIfUnambiguous(name);
        return name;
    }

    // Old names turned every other character into '_'. Move the file only when one world could have written it.
    private void moveLegacyFileIfUnambiguous(String currentName) {
        Path directory = chestDirectory;
        if (directory == null || world == null) {
            return;
        }
        String legacyWorld = sanitize(world);
        String legacyName = legacyWorld + "_" + x + "_" + y + "_" + z + ".json";
        if (legacyName.equals(currentName)) {
            return;
        }
        synchronized (ChestKey.class) {
            try {
                Path from = directory.resolve(legacyName);
                Path to = directory.resolve(currentName);
                if (Files.exists(to) || !Files.exists(from) || !legacyIsUnambiguous(legacyWorld)) {
                    return;
                }
                Files.move(from, to);
            } catch (IOException | RuntimeException ignored) {
                // A failed move must not stop later chest saves.
            }
        }
    }

    private boolean legacyIsUnambiguous(String legacyWorld) throws IOException {
        Universe universe = Universe.get();
        if (universe == null) {
            return false;
        }
        Path worldsPath = universe.getWorldsPath();
        if (worldsPath == null || !Files.isDirectory(worldsPath)) {
            return false;
        }
        Set<String> owners = new HashSet<>();
        for (World loaded : universe.getWorlds().values()) {
            addOwner(owners, legacyWorld, loaded.getName());
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldsPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    addOwner(owners, legacyWorld, path.getFileName().toString());
                }
            }
        }
        return owners.isEmpty() || (owners.size() == 1 && owners.contains(world));
    }

    private static void addOwner(Set<String> owners, String legacyWorld, String name) {
        if (name != null && legacyWorld.equals(sanitize(name))) {
            owners.add(name);
        }
    }

    /** Keeps letters, digits, '_' and '-'. Every other character is percent-encoded so it cannot become '_'. */
    private static String encodeWorld(String world) {
        StringBuilder out = new StringBuilder(world.length());
        for (int i = 0; i < world.length(); i++) {
            char c = world.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                out.append(c);
            } else {
                out.append('%').append(String.format("%04x", (int) c));
            }
        }
        return out.toString();
    }

    private static String sanitize(String world) {
        return world.replaceAll("[^A-Za-z0-9_-]", "_");
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
