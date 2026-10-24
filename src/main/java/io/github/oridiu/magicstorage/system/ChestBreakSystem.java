package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import io.github.oridiu.magicstorage.storage.ChestBlock;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * When a Magic Chest is broken, its contents fall out like from a normal
 * chest and the chest's storage is deleted. Linked bags then report that
 * the chest is gone. Breaking either half of a large chest breaks all of it,
 * like in vanilla.
 */
public class ChestBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public ChestBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event
    ) {
        if (event.isCancelled() || !MagicItems.isChest(event.getBlockType())) {
            return;
        }
        World world = store.getExternalData().getWorld();
        ChestBlock chest = ChestBlock.find(world, event.getTargetBlock());
        if (chest == null) {
            return;
        }
        List<ItemStack> items = MagicStoragePlugin.getInstance().getStorage().remove(chest.key(world));
        if (items.isEmpty()) {
            return;
        }
        Vector3i pos = chest.origin();
        Vector3d dropPos = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(commandBuffer, items, dropPos, Rotation3f.IDENTITY);
        commandBuffer.addEntities(drops, AddReason.SPAWN);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
