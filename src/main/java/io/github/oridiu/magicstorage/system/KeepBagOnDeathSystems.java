package io.github.oridiu.magicstorage.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the Magic Bag on death: it is taken out of the inventory right
 * before the death drops are made and put back right after. A bag that
 * still does not fit is dropped at the player's feet.
 */
public final class KeepBagOnDeathSystems {

    private static final Map<Ref<EntityStore>, List<ItemStack>> STASHED = new ConcurrentHashMap<>();

    private KeepBagOnDeathSystems() {
    }

    public static class Stash extends DeathSystems.OnDeathSystem {

        private static final Set<Dependency<EntityStore>> DEPENDENCIES =
                Set.of(new SystemDependency<>(Order.BEFORE, DeathSystems.DropPlayerDeathItems.class));

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return DEPENDENCIES;
        }

        @Override
        public void onComponentAdded(
                @Nonnull Ref<EntityStore> ref,
                @Nonnull DeathComponent component,
                @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            CombinedItemContainer inventory = InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING);
            List<ItemStack> bags = new ArrayList<>();
            for (short slot = 0; slot < inventory.getCapacity(); slot++) {
                ItemStack stack = inventory.getItemStack(slot);
                if (MagicItems.isBag(stack)) {
                    bags.add(stack);
                    inventory.removeItemStackFromSlot(slot);
                }
            }
            if (!bags.isEmpty()) {
                STASHED.put(ref, bags);
            }
        }
    }

    public static class Restore extends DeathSystems.OnDeathSystem {

        private static final Set<Dependency<EntityStore>> DEPENDENCIES =
                Set.of(new SystemDependency<>(Order.AFTER, DeathSystems.DropPlayerDeathItems.class));

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return DEPENDENCIES;
        }

        @Override
        public void onComponentAdded(
                @Nonnull Ref<EntityStore> ref,
                @Nonnull DeathComponent component,
                @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            List<ItemStack> bags = STASHED.remove(ref);
            if (bags == null) {
                return;
            }
            CombinedItemContainer inventory = InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING);
            List<ItemStack> leftover = new ArrayList<>();
            for (ItemStack bag : bags) {
                ItemStack remainder = inventory.addItemStack(bag).getRemainder();
                if (!ItemStack.isEmpty(remainder)) {
                    leftover.add(remainder);
                }
            }
            if (leftover.isEmpty()) {
                return;
            }
            TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            Vector3d dropPos = new Vector3d(transform.getPosition());
            Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(commandBuffer, leftover, dropPos, Rotation3f.IDENTITY);
            commandBuffer.addEntities(drops, AddReason.SPAWN);
        }
    }
}
