package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import io.github.oridiu.magicstorage.storage.ChestKey;
import io.github.oridiu.magicstorage.storage.ChestStorage;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Shows a glow above the Magic Chest a player's bag is linked to. The
 * particles are sent to that player only, so everyone sees just their own chest.
 */
public class LinkedChestMarkerSystem extends DelayedEntitySystem<EntityStore> {

    public static final String PARTICLE_SYSTEM = "Magic_Chest_Marker";
    private static final float INTERVAL = 2.0F;
    /** Slightly longer than the interval, so the glow does not flicker between two spawns. */
    private static final float PARTICLE_DURATION = INTERVAL + 0.5F;
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
        ChestKey link = linkedChest(InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING));
        if (link == null) {
            return;
        }
        ChestStorage storage = plugin.getStorage();
        ChestKey chest = storage.resolve(link);
        if (!chest.world().equals(store.getExternalData().getWorld().getName())) {
            return;
        }
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d marker = new Vector3d(chest.x() + 0.5, chest.y() + 1.0, chest.z() + 0.5);
        ChestKey partner = storage.partnerOf(chest);
        if (partner != null) {
            marker.add((partner.x() - chest.x()) * 0.5, 0.0, (partner.z() - chest.z()) * 0.5);
        }
        if (transform.getPosition().distanceSquared(marker) > RANGE * RANGE || storage.get(chest) == null) {
            return;
        }
        ParticleUtil.spawnParticleEffect(
                PARTICLE_SYSTEM, marker.x, marker.y, marker.z, 0.0F, 0.0F, 0.0F, 1.0F, null, null, List.of(ref), commandBuffer, PARTICLE_DURATION);
    }

    @Nullable
    private static ChestKey linkedChest(@Nonnull CombinedItemContainer inventory) {
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (MagicItems.isBag(stack)) {
                ChestKey link = stack.getFromMetadataOrNull(ChestKey.BAG_LINK);
                if (link != null) {
                    return link;
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
