package io.github.oridiu.magicstorage.system;

import com.google.gson.JsonElement;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ExtraResources;
import com.hypixel.hytale.protocol.ItemQuantity;
import com.hypixel.hytale.protocol.packets.window.UpdateWindow;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.bench.Bench;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.BlockWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.MaterialExtraResourcesSection;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.DelegateItemContainer;
import com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import io.github.oridiu.magicstorage.storage.ChestKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets crafting benches use the items in the Magic Chest a player's bag is
 * linked to, as if the chest stood next to the bench.
 * <p>
 * A bench window collects the chests around it into its "extra resources"
 * whenever they are invalidated (on open and after every craft). This system
 * adds the linked chest to that list on the next tick and sends the new list
 * to the player. Items are only taken out, never put in.
 */
public class BagCraftingSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        MagicStoragePlugin plugin = MagicStoragePlugin.getInstance();
        if (!plugin.getSettings().isCraftFromBag()) {
            return;
        }
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        for (Window window : player.getWindowManager().getWindows()) {
            if (!(window instanceof BlockWindow blockWindow) || !(window instanceof MaterialContainerWindow materials)) {
                continue;
            }
            Bench bench = blockWindow.getBlockType().getBench();
            if (bench == null) {
                continue;
            }
            // Only look at the section while it is valid: reading an invalid one refills it, and then the game no longer sends it to the client.
            if (materials.isValid() && materials.getExtraResourcesSection().getItemContainer() instanceof BagChestContainer) {
                continue;
            }
            SimpleItemContainer chest = linkedChest(archetypeChunk, index);
            if (chest == null) {
                continue;
            }
            MaterialExtraResourcesSection section = materials.getExtraResourcesSection();
            addChest(section, chest, bench, tierLevel(window));
            ExtraResources extraResources = section.toPacket();
            playerRef.getPacketHandler().writeNoCache(new UpdateWindow(
                    window.getId(),
                    window.getData().toString(),
                    window instanceof ItemContainerWindow items ? items.getItemContainer().toPacket() : null,
                    extraResources));
        }
    }

    @Nullable
    private static SimpleItemContainer linkedChest(@Nonnull ArchetypeChunk<EntityStore> archetypeChunk, int index) {
        ChestKey link = LinkedChestMarkerSystem.linkedChest(archetypeChunk, index);
        return link != null ? MagicStoragePlugin.getInstance().getStorage().get(link) : null;
    }

    /** Adds the chest's items to what the bench can use, the same way the game adds the chests around a bench. */
    private static void addChest(@Nonnull MaterialExtraResourcesSection section, @Nonnull SimpleItemContainer chest, @Nonnull Bench bench, int tierLevel) {
        ItemContainer nearby = section.getItemContainer() != null ? section.getItemContainer() : EmptyItemContainer.INSTANCE;
        DelegateItemContainer<SimpleItemContainer> takeOnly = new DelegateItemContainer<>(chest);
        takeOnly.setGlobalFilter(FilterType.ALLOW_OUTPUT_ONLY);

        Map<String, ItemQuantity> totals = new LinkedHashMap<>();
        ItemQuantity[] current = section.toPacket().resources;
        if (current != null) {
            for (ItemQuantity quantity : current) {
                totals.put(quantity.itemId, new ItemQuantity(quantity));
            }
        }
        chest.forEach((slot, stack) -> {
            if (CraftingPlugin.isValidCraftingMaterialForBench(bench, stack)
                    || CraftingPlugin.isValidUpgradeMaterialForBench(bench, tierLevel, stack)) {
                totals.computeIfAbsent(stack.getItemId(), id -> new ItemQuantity(id, 0)).quantity += stack.getQuantity();
            }
        });

        section.setItemContainer(new BagChestContainer(nearby, takeOnly));
        section.setExtraMaterials(totals.values().toArray(new ItemQuantity[0]));
    }

    private static int tierLevel(@Nonnull Window window) {
        JsonElement tier = window.getData().get("tierLevel");
        return tier != null && tier.isJsonPrimitive() ? tier.getAsInt() : 1;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(Player.getComponentType(), PlayerRef.getComponentType());
    }

    /** Nearby chests plus the linked Magic Chest; its type tells that the chest was added already. */
    private static final class BagChestContainer extends CombinedItemContainer {
        private BagChestContainer(ItemContainer... containers) {
            super(containers);
        }
    }
}
