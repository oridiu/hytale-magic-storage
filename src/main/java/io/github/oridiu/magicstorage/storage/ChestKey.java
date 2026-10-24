package io.github.oridiu.magicstorage.storage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

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
        return world.replaceAll("[^A-Za-z0-9_-]", "_") + "_" + x + "_" + y + "_" + z + ".json";
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
