package io.github.oridiu.magicstorage.storage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import io.github.oridiu.magicstorage.MagicItems;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A Magic Chest as it stands in the world. A large chest is one block with a
 * filler block next to it; its storage is kept under the main block, the
 * filler block's position is its {@code partner}.
 *
 * @param origin     the chest's main block
 * @param partner    the filler half of a large chest, null for a small chest
 * @param blockType  block type at the main block
 * @param rotation   rotation index of the main block
 */
public record ChestBlock(@Nonnull Vector3i origin, @Nullable Vector3i partner, @Nonnull BlockType blockType, int rotation) {

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public boolean isLarge() {
        return partner != null;
    }

    @Nonnull
    public ChestKey key(@Nonnull World world) {
        return ChestKey.of(world.getName(), origin);
    }

    @Nullable
    public ChestKey partnerKey(@Nonnull World world) {
        return partner != null ? ChestKey.of(world.getName(), partner) : null;
    }

    /** Finds the Magic Chest at this position (either half of a large one), or null. */
    @Nullable
    public static ChestBlock find(@Nonnull World world, @Nonnull Vector3i pos) {
        ChunkStore chunkStore = world.getChunkStore();
        BlockSection section = section(chunkStore, pos.x, pos.y, pos.z);
        if (section == null) {
            return null;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(section.get(pos.x, pos.y, pos.z));
        if (!MagicItems.isChest(blockType)) {
            return null;
        }
        Vector3i origin = originOf(section, pos);
        if (!origin.equals(pos)) {
            section = section(chunkStore, origin.x, origin.y, origin.z);
            if (section == null) {
                return null;
            }
            blockType = BlockType.getAssetMap().getAsset(section.get(origin.x, origin.y, origin.z));
            if (!MagicItems.isChest(blockType)) {
                return null;
            }
        }
        int rotation = section.getRotationIndex(origin.x, origin.y, origin.z);
        if (!MagicItems.isLargeChest(blockType)) {
            return new ChestBlock(origin, null, blockType, rotation);
        }
        for (int[] side : SIDES) {
            Vector3i neighbour = new Vector3i(origin.x + side[0], origin.y, origin.z + side[1]);
            BlockSection neighbourSection = section(chunkStore, neighbour.x, neighbour.y, neighbour.z);
            if (neighbourSection == null) {
                continue;
            }
            BlockType neighbourType = BlockType.getAssetMap().getAsset(neighbourSection.get(neighbour.x, neighbour.y, neighbour.z));
            if (MagicItems.isLargeChest(neighbourType) && originOf(neighbourSection, neighbour).equals(origin)) {
                return new ChestBlock(origin, neighbour, blockType, rotation);
            }
        }
        // The other half is not loaded (or gone); treat it like a small chest for now.
        return new ChestBlock(origin, null, blockType, rotation);
    }

    private static Vector3i originOf(@Nonnull BlockSection section, @Nonnull Vector3i pos) {
        int filler = section.getFiller(pos.x, pos.y, pos.z);
        if (filler == FillerBlockUtil.NO_FILLER) {
            return pos;
        }
        return new Vector3i(pos).sub(FillerBlockUtil.unpackX(filler), FillerBlockUtil.unpackY(filler), FillerBlockUtil.unpackZ(filler));
    }

    @Nullable
    private static BlockSection section(@Nonnull ChunkStore chunkStore, int x, int y, int z) {
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return null;
        }
        return chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
    }
}
