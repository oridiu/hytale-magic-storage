package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.storage.ChestKey;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A player may carry only one Magic Bag. */
public final class OneBagSystems {

    /** How long a rejected bag stays on the ground before it can be picked up again. */
    private static final float REJECTED_PICKUP_DELAY = 5.0F;

    private OneBagSystems() {
    }

    static int countBags(@Nonnull CombinedItemContainer inventory) {
        return inventory.countItemStacks(MagicItems::isBag);
    }

    /** Drops every bag beyond the first one. A linked bag wins over an unlinked one. */
    public static class LimitInventory extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

        public LimitInventory() {
            super(InventoryChangeEvent.class);
        }

        @Override
        public void handle(
                int index,
                @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer,
                @Nonnull InventoryChangeEvent event
        ) {
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            CombinedItemContainer inventory = InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING);
            if (countBags(inventory) <= 1) {
                return;
            }

            short keep = -1;
            for (short slot = 0; slot < inventory.getCapacity(); slot++) {
                ItemStack stack = inventory.getItemStack(slot);
                if (MagicItems.isBag(stack) && (keep < 0 || stack.getFromMetadataOrNull(ChestKey.BAG_LINK) != null
                        && inventory.getItemStack(keep).getFromMetadataOrNull(ChestKey.BAG_LINK) == null)) {
                    keep = slot;
                }
            }

            TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
            for (short slot = 0; slot < inventory.getCapacity(); slot++) {
                ItemStack stack = inventory.getItemStack(slot);
                if (slot == keep || !MagicItems.isBag(stack)) {
                    continue;
                }
                inventory.removeItemStackFromSlot(slot);
                if (transform != null) {
                    drop(commandBuffer, stack, transform.getPosition());
                }
            }

            PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                playerRef.sendMessage(Message.translation("server.magicstorage.bag.onlyOne").color("#ff5555"));
            }
        }

        private static void drop(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ItemStack stack, @Nonnull Vector3d position) {
            Holder<EntityStore> holder = ItemComponent.generateItemDrop(
                    commandBuffer, stack, new Vector3d(position).add(0.0, 1.0, 0.0), Rotation3f.IDENTITY, 0.0F, 3.25F, 0.0F);
            if (holder == null) {
                return;
            }
            ItemComponent item = holder.getComponent(ItemComponent.getComponentType());
            if (item != null) {
                item.setPickupDelay(REJECTED_PICKUP_DELAY);
            }
            commandBuffer.addEntity(holder, AddReason.SPAWN);
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }
    }

    /** Refuses to craft a Magic Bag while the player already carries one. */
    public static class BlockCrafting extends EntityEventSystem<EntityStore, CraftRecipeEvent.Pre> {

        public BlockCrafting() {
            super(CraftRecipeEvent.Pre.class);
        }

        @Override
        public void handle(
                int index,
                @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer,
                @Nonnull CraftRecipeEvent.Pre event
        ) {
            MaterialQuantity output = event.getCraftedRecipe().getPrimaryOutput();
            if (output == null || !MagicItems.BAG_ID.equals(output.getItemId())) {
                return;
            }
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            CombinedItemContainer inventory = InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING);
            if (countBags(inventory) == 0) {
                return;
            }
            event.setCancelled(true);
            PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                playerRef.sendMessage(Message.translation("server.magicstorage.bag.onlyOne").color("#ff5555"));
            }
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }
    }
}
