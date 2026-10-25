package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import io.github.oridiu.magicstorage.storage.ChestBlock;
import io.github.oridiu.magicstorage.storage.ChestKey;
import io.github.oridiu.magicstorage.storage.ChestStorage;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Marks the Magic Chest a player's bag is linked to: blue sparks rise from the
 * lid and the gem on the front glows. The particles are sent to that player
 * only, so everyone sees just their own chest.
 */
public class LinkedChestMarkerSystem extends DelayedEntitySystem<EntityStore> {

    public static final String PARTICLE_SYSTEM = "Magic_Chest_Marker";
    public static final String GEM_GLOW_SYSTEM = "Magic_Chest_Gem_Glow";
    private static final float INTERVAL = 2.0F;
    /**
     * The glow's particles outlive their spawner, so each spawn only needs to emit
     * until the next one starts; a longer duration would overlap two spawns and
     * make the glow flash brighter.
     */
    private static final float PARTICLE_DURATION = INTERVAL;
    /** On the surface of the front gem, in blocks from the bottom centre of an unrotated chest (from Magic_Chest.blockymodel), so a bit over half the glow shows in front of it. */
    private static final Vector3d GEM_OFFSET = new Vector3d(0.0, 0.5625, 0.52);
    private static final double RANGE = 64.0;

    public LinkedChestMarkerSystem() {
        super(INTERVAL);
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        MagicStoragePlugin plugin = MagicStoragePlugin.getInstance();
        if (!plugin.getSettings().isShowLinkedChest()) {
            return;
        }
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        ChestKey link = linkedChest(archetypeChunk, index);
        if (link == null) {
            return;
        }
        ChestStorage storage = plugin.getStorage();
        ChestKey chest = storage.resolve(link);
        World world = store.getExternalData().getWorld();
        if (!chest.world().equals(world.getName())) {
            return;
        }
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        // Bottom centre of the chest; for a large chest, between its two halves.
        Vector3d base = new Vector3d(chest.x() + 0.5, chest.y(), chest.z() + 0.5);
        ChestKey partner = storage.partnerOf(chest);
        if (partner != null) {
            base.add((partner.x() - chest.x()) * 0.5, 0.0, (partner.z() - chest.z()) * 0.5);
        }
        if (transform.getPosition().distanceSquared(base) > RANGE * RANGE || storage.get(chest) == null) {
            return;
        }
        ParticleUtil.spawnParticleEffect(
                PARTICLE_SYSTEM, base.x, base.y + 1.0, base.z, 0.0F, 0.0F, 0.0F, 1.0F, null, null, List.of(ref), commandBuffer, PARTICLE_DURATION);
        ChestBlock block = ChestBlock.find(world, new Vector3i(chest.x(), chest.y(), chest.z()));
        if (block != null) {
            Vector3d gem = RotationTuple.get(block.rotation()).rotatedVector(new Vector3d(GEM_OFFSET)).add(base);
            ParticleUtil.spawnParticleEffect(
                    GEM_GLOW_SYSTEM, gem.x, gem.y, gem.z, 0.0F, 0.0F, 0.0F, 1.0F, null, null, List.of(ref), commandBuffer, PARTICLE_DURATION);
        }
    }

    /**
     * The chest the bag this player carries right now is linked to. Reads the
     * inventories straight from the player's components on every tick, so a
     * bag that was just handed to another player marks the chest for its new
     * holder only.
     */
    @Nullable
    private static ChestKey linkedChest(@Nonnull ArchetypeChunk<EntityStore> archetypeChunk, int index) {
        for (ComponentType<EntityStore, ? extends InventoryComponent> type : InventoryComponent.EVERYTHING) {
            if (!archetypeChunk.getArchetype().contains(type)) {
                continue;
            }
            InventoryComponent component = archetypeChunk.getComponent(index, type);
            ItemContainer inventory = component != null ? component.getInventory() : null;
            if (inventory == null) {
                continue;
            }
            for (short slot = 0; slot < inventory.getCapacity(); slot++) {
                ItemStack stack = inventory.getItemStack(slot);
                if (MagicItems.isBag(stack)) {
                    ChestKey link = stack.getFromMetadataOrNull(ChestKey.BAG_LINK);
                    if (link != null) {
                        return link;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(Player.getComponentType(), TransformComponent.getComponentType());
    }
}
