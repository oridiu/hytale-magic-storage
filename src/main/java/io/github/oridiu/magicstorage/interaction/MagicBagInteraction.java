package io.github.oridiu.magicstorage.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.DelegateItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicItems;
import io.github.oridiu.magicstorage.MagicStoragePlugin;
import io.github.oridiu.magicstorage.storage.ChestKey;
import io.github.oridiu.magicstorage.storage.ChestStorage;

import javax.annotation.Nonnull;

/**
 * Right click with a Magic Bag: opens the linked Magic Chest, wherever the
 * player is. Unlike a chest window, this window has no distance check.
 */
public class MagicBagInteraction extends SimpleInstantInteraction {

    public static final String TYPE = "MagicStorageBag";

    public static final BuilderCodec<MagicBagInteraction> CODEC = BuilderCodec.builder(
                    MagicBagInteraction.class, MagicBagInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Opens the Magic Chest the held Magic Bag is linked to.")
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }
        Ref<EntityStore> ref = context.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        PageManager pageManager = player.getPageManager();
        if (pageManager.getCustomPage() != null) {
            return;
        }

        ItemStack bag = context.getHeldItem();
        if (!MagicItems.isBag(bag)) {
            return;
        }
        ChestKey key = bag.getFromMetadataOrNull(ChestKey.BAG_LINK);
        if (key == null) {
            playerRef.sendMessage(Message.translation("server.magicstorage.bag.notLinked"));
            return;
        }

        MagicStoragePlugin plugin = MagicStoragePlugin.getInstance();
        ChestStorage storage = plugin.getStorage();
        if (storage.get(key) == null) {
            playerRef.sendMessage(Message.translation("server.magicstorage.bag.chestMissing").color("#ff5555"));
            return;
        }
        // Grows the chest if the configured size went up since it was last opened.
        ChestKey chestKey = storage.resolve(key);
        short capacity = storage.partnerOf(chestKey) != null
                ? plugin.getSettings().getLargeChestCapacity()
                : plugin.getSettings().getChestCapacity();
        SimpleItemContainer container = storage.getOrCreate(chestKey, capacity);

        ItemContainer view = container;
        if (!plugin.getSettings().isAllowDepositViaBag()) {
            DelegateItemContainer<SimpleItemContainer> takeOnly = new DelegateItemContainer<>(container);
            takeOnly.setGlobalFilter(FilterType.ALLOW_OUTPUT_ONLY);
            view = takeOnly;
        }
        ContainerWindow window = new ContainerWindow(view);
        window.getData().addProperty("blockItemId", MagicItems.CHEST_ID);
        window.registerCloseEvent(event -> storage.saveDirty());
        pageManager.setPageWithWindows(ref, ref.getStore(), Page.Bench, true, window);
    }
}
