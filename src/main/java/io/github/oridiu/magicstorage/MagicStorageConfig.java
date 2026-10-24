package io.github.oridiu.magicstorage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Settings of the mod, stored in {@code mods/oridiu_MagicStorage/config.json}
 * on the server.
 */
public class MagicStorageConfig {

    public static final BuilderCodec<MagicStorageConfig> CODEC = BuilderCodec.builder(MagicStorageConfig.class, MagicStorageConfig::new)
            .append(new KeyedCodec<>("AllowDepositViaBag", Codec.BOOLEAN),
                    (config, value) -> config.allowDepositViaBag = value,
                    config -> config.allowDepositViaBag)
            .documentation("If false, the Magic Bag only lets players take items out of the linked chest.")
            .add()
            .append(new KeyedCodec<>("ChestCapacity", Codec.INTEGER),
                    (config, value) -> config.chestCapacity = value,
                    config -> config.chestCapacity)
            .documentation("Number of slots of a newly created Magic Chest.")
            .add()
            .append(new KeyedCodec<>("AutoSaveSeconds", Codec.INTEGER),
                    (config, value) -> config.autoSaveSeconds = value,
                    config -> config.autoSaveSeconds)
            .documentation("How often changed chests are written to disk.")
            .add()
            .build();

    private boolean allowDepositViaBag = true;
    private int chestCapacity = 36;
    private int autoSaveSeconds = 30;

    public boolean isAllowDepositViaBag() {
        return allowDepositViaBag;
    }

    public short getChestCapacity() {
        return (short) Math.clamp(chestCapacity, 9, 180);
    }

    public int getAutoSaveSeconds() {
        return Math.max(5, autoSaveSeconds);
    }
}
