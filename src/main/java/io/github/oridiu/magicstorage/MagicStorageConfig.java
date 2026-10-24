package io.github.oridiu.magicstorage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Settings of the mod, stored in {@code mods/oridiu_MagicStorage/config.json}
 * on the server. Operators can also change them in game with
 * {@code /magicstorage}.
 */
public class MagicStorageConfig {

    public static final int MIN_CAPACITY = 9;
    public static final int MAX_CAPACITY = 90;
    public static final int MIN_AUTO_SAVE_SECONDS = 5;

    public static final BuilderCodec<MagicStorageConfig> CODEC = BuilderCodec.builder(MagicStorageConfig.class, MagicStorageConfig::new)
            .append(new KeyedCodec<>("AllowDepositViaBag", Codec.BOOLEAN),
                    (config, value) -> config.allowDepositViaBag = value,
                    config -> config.allowDepositViaBag)
            .documentation("If false, the Magic Bag only lets players take items out of the linked chest.")
            .add()
            .append(new KeyedCodec<>("ChestCapacity", Codec.INTEGER),
                    (config, value) -> config.chestCapacity = value,
                    config -> config.chestCapacity)
            .documentation("Number of slots of a Magic Chest. A large Magic Chest has twice as many. Chests only grow, they never shrink.")
            .add()
            .append(new KeyedCodec<>("ShowLinkedChest", Codec.BOOLEAN),
                    (config, value) -> config.showLinkedChest = value,
                    config -> config.showLinkedChest)
            .documentation("If true, a player carrying a linked Magic Bag sees a glow above the linked chest. Only that player sees it.")
            .add()
            .append(new KeyedCodec<>("AutoSaveSeconds", Codec.INTEGER),
                    (config, value) -> config.autoSaveSeconds = value,
                    config -> config.autoSaveSeconds)
            .documentation("How often changed chests are written to disk.")
            .add()
            .build();

    private boolean allowDepositViaBag = true;
    // Same as a vanilla small chest; two of them make a large chest with 36.
    private int chestCapacity = 18;
    private boolean showLinkedChest = true;
    private int autoSaveSeconds = 30;

    public boolean isAllowDepositViaBag() {
        return allowDepositViaBag;
    }

    public void setAllowDepositViaBag(boolean allowDepositViaBag) {
        this.allowDepositViaBag = allowDepositViaBag;
    }

    public short getChestCapacity() {
        return (short) Math.clamp(chestCapacity, MIN_CAPACITY, MAX_CAPACITY);
    }

    public void setChestCapacity(int chestCapacity) {
        this.chestCapacity = Math.clamp(chestCapacity, MIN_CAPACITY, MAX_CAPACITY);
    }

    /** Capacity of a large chest made of two Magic Chests. */
    public short getLargeChestCapacity() {
        return (short) (getChestCapacity() * 2);
    }

    public boolean isShowLinkedChest() {
        return showLinkedChest;
    }

    public void setShowLinkedChest(boolean showLinkedChest) {
        this.showLinkedChest = showLinkedChest;
    }

    public int getAutoSaveSeconds() {
        return Math.max(MIN_AUTO_SAVE_SECONDS, autoSaveSeconds);
    }

    public void setAutoSaveSeconds(int autoSaveSeconds) {
        this.autoSaveSeconds = Math.max(MIN_AUTO_SAVE_SECONDS, autoSaveSeconds);
    }
}
