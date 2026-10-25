package io.github.oridiu.magicstorage.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import io.github.oridiu.magicstorage.storage.ChestBlock;
import io.github.oridiu.magicstorage.storage.ChestKey;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Use" on a Magic Chest (small or large). With a Magic Bag in hand it links
 * the bag to this chest, otherwise it opens the chest like a normal one.
 */
public class MagicChestInteraction extends SimpleBlockInteraction {

    public static final String TYPE = "MagicStorageChest";

    public static final BuilderCodec<MagicChestInteraction> CODEC = BuilderCodec.builder(
                    MagicChestInteraction.class, MagicChestInteraction::new, SimpleBlockInteraction.CODEC)
            .documentation("Opens a Magic Chest, or links the Magic Bag in hand to it.")
            .build();

    /** Players looking into each chest, to play the open and close animation. */
    private static final Map<ChestKey, Set<UUID>> VIEWERS = new ConcurrentHashMap<>();

    @Override
    protected void interactWithBlock(
            @Nonnull World world,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nullable ItemStack itemInHand,
            @Nonnull Vector3i pos,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> ref = context.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        ChestBlock chest = ChestBlock.find(world, pos);
        if (chest == null) {
            return;
        }
        ChestKey key = chest.key(world);
        if (MagicItems.isBag(itemInHand)) {
            linkBag(context, key, playerRef);
            return;
        }
        SimpleItemContainer container = openStorage(world, chest);
        if (container == null) {
            playerRef.sendMessage(Message.translation("server.magicstorage.chest.unreadable").color("#ff5555"));
            return;
        }

        UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        UUID uuid = uuidComponent.getUuid();
        Vector3i origin = chest.origin();
        ContainerBlockWindow window = new ContainerBlockWindow(origin.x, origin.y, origin.z, chest.rotation(), chest.blockType(), container);
        Store<EntityStore> store = ref.getStore();
        if (!player.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, window)) {
            return;
        }

        Set<UUID> viewers = VIEWERS.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        if (viewers.add(uuid) && viewers.size() == 1) {
            setChestState(world, origin, "OpenWindow");
        }
        window.registerCloseEvent(event -> {
            MagicStoragePlugin.getInstance().getStorage().saveDirty();
            viewers.remove(uuid);
            if (viewers.isEmpty()) {
                VIEWERS.remove(key, viewers);
                setChestState(world, origin, "CloseWindow");
            }
        });
    }

    /**
     * Returns the chest's storage. For a large chest this also joins the two
     * halves, in case that did not happen when it was placed.
     */
    @Nullable
    public static SimpleItemContainer openStorage(@Nonnull World world, @Nonnull ChestBlock chest) {
        MagicStoragePlugin plugin = MagicStoragePlugin.getInstance();
        ChestKey partner = chest.partnerKey(world);
        if (partner != null) {
            return plugin.getStorage().merge(chest.key(world), partner, plugin.getSettings().getLargeChestCapacity());
        }
        return plugin.getStorage().getOrCreate(chest.key(world), plugin.getSettings().getChestCapacity());
    }

    private static void linkBag(@Nonnull InteractionContext context, @Nonnull ChestKey key, @Nonnull PlayerRef playerRef) {
        ItemContainer heldContainer = context.getHeldItemContainer();
        if (heldContainer == null) {
            return;
        }
        short slot = context.getHeldItemSlot();
        ItemStack bag = heldContainer.getItemStack(slot);
        if (!MagicItems.isBag(bag)) {
            return;
        }
        ChestKey previous = bag.getFromMetadataOrNull(ChestKey.BAG_LINK);
        if (key.equals(previous)) {
            playerRef.sendMessage(Message.translation("server.magicstorage.bag.alreadyLinked"));
            return;
        }
        heldContainer.replaceItemStackInSlot(slot, bag, bag.withMetadata(ChestKey.BAG_LINK, key));
        playerRef.sendMessage(Message.translation("server.magicstorage.bag.linked").color("#b48cff"));
    }

    /** Plays the lid animation, if the chest is still there. */
    private static void setChestState(@Nonnull World world, @Nonnull Vector3i pos, @Nonnull String state) {
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return;
        }
        BlockSection section = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        if (section == null) {
            return;
        }
        BlockType current = BlockType.getAssetMap().getAsset(section.get(pos.x, pos.y, pos.z));
        if (MagicItems.isChest(current)) {
            BlockOperations.setBlockInteractionState(chunkStore, sectionRef, pos.x, pos.y, pos.z, current, state, false);
        }
    }

    @Override
    protected void simulateInteractWithBlock(
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nullable ItemStack itemInHand,
            @Nonnull World world,
            @Nonnull Vector3i targetBlock
    ) {
    }
}
