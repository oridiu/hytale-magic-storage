package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import io.github.oridiu.magicstorage.interaction.MagicChestInteraction;
import io.github.oridiu.magicstorage.storage.ChestBlock;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Creates the storage of a newly placed Magic Chest, so bags can be linked to
 * it before anything was put in. Placing a Magic Chest next to another one
 * turns both into a large chest (the block rules in Magic_Chest.json do that);
 * once the blocks are set, this joins the two chests' contents.
 */
public class ChestPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public ChestPlaceSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event
    ) {
        ItemStack item = event.getItemInHand();
        if (event.isCancelled() || ItemStack.isEmpty(item) || !MagicItems.CHEST_ID.equals(item.getItemId())) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Vector3i pos = new Vector3i(event.getTargetBlock());
        // The event comes before the block is set; check once the placement is done.
        world.execute(() -> {
            ChestBlock chest = ChestBlock.find(world, pos);
            if (chest != null) {
                MagicChestInteraction.openStorage(world, chest);
                MagicStoragePlugin.getInstance().getStorage().saveDirty();
            }
        });
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
