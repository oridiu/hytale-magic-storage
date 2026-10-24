package io.github.oridiu.magicstorage.command;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicStorageConfig;
import io.github.oridiu.magicstorage.MagicStoragePlugin;

import javax.annotation.Nonnull;

/** Settings window opened by {@code /magicstorage settings}. */
public class SettingsPage extends InteractiveCustomUIPage<SettingsPage.Data> {

    public SettingsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        MagicStorageConfig config = MagicStoragePlugin.getInstance().getSettings();
        commandBuilder.append("Pages/MagicStorageSettings.ui");
        commandBuilder.set("#AllowDeposit #CheckBox.Value", config.isAllowDepositViaBag());
        commandBuilder.set("#ShowLinkedChest #CheckBox.Value", config.isShowLinkedChest());
        commandBuilder.set("#ChestCapacity.Value", (int) config.getChestCapacity());
        commandBuilder.set("#AutoSaveSeconds.Value", config.getAutoSaveSeconds());
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                new EventData()
                        .append(Data.ALLOW_DEPOSIT, "#AllowDeposit #CheckBox.Value")
                        .append(Data.SHOW_LINKED_CHEST, "#ShowLinkedChest #CheckBox.Value")
                        .append(Data.CHEST_CAPACITY, "#ChestCapacity.Value")
                        .append(Data.AUTO_SAVE_SECONDS, "#AutoSaveSeconds.Value"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        MagicStoragePlugin.getInstance().updateSettings(config -> {
            config.setAllowDepositViaBag(data.allowDeposit);
            config.setShowLinkedChest(data.showLinkedChest);
            config.setChestCapacity((int) Math.round(data.chestCapacity));
            config.setAutoSaveSeconds((int) Math.round(data.autoSaveSeconds));
        });
        playerRef.sendMessage(Message.translation("server.magicstorage.settings.saved").color("#b48cff"));
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    public static class Data {
        static final String ALLOW_DEPOSIT = "@AllowDeposit";
        static final String SHOW_LINKED_CHEST = "@ShowLinkedChest";
        static final String CHEST_CAPACITY = "@ChestCapacity";
        static final String AUTO_SAVE_SECONDS = "@AutoSaveSeconds";

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .addField(new KeyedCodec<>(ALLOW_DEPOSIT, Codec.BOOLEAN), (data, value) -> data.allowDeposit = value, data -> data.allowDeposit)
                .addField(new KeyedCodec<>(SHOW_LINKED_CHEST, Codec.BOOLEAN), (data, value) -> data.showLinkedChest = value, data -> data.showLinkedChest)
                .addField(new KeyedCodec<>(CHEST_CAPACITY, Codec.DOUBLE), (data, value) -> data.chestCapacity = value, data -> data.chestCapacity)
                .addField(new KeyedCodec<>(AUTO_SAVE_SECONDS, Codec.DOUBLE), (data, value) -> data.autoSaveSeconds = value, data -> data.autoSaveSeconds)
                .build();

        private boolean allowDeposit;
        private boolean showLinkedChest;
        private double chestCapacity;
        private double autoSaveSeconds;
    }
}
