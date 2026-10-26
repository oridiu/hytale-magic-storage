package io.github.oridiu.magicstorage.command;

import io.github.oridiu.magicstorage.MagicStorageConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The settings operators can change in game, by their config.json names. */
enum Setting {

    ALLOW_DEPOSIT_VIA_BAG("AllowDepositViaBag", "deposit", true,
            config -> String.valueOf(config.isAllowDepositViaBag()),
            (config, value) -> config.setAllowDepositViaBag(value != 0)),
    CHEST_CAPACITY("ChestCapacity", "capacity", false,
            config -> String.valueOf(config.getChestCapacity()),
            (config, value) -> config.setChestCapacity(value)),
    SHOW_LINKED_CHEST("ShowLinkedChest", "glow", true,
            config -> String.valueOf(config.isShowLinkedChest()),
            (config, value) -> config.setShowLinkedChest(value != 0)),
    CRAFT_FROM_BAG("CraftFromBag", "craft", true,
            config -> String.valueOf(config.isCraftFromBag()),
            (config, value) -> config.setCraftFromBag(value != 0));

    final String key;
    final String shortName;
    private final boolean flag;
    private final Function<MagicStorageConfig, String> getter;
    private final BiConsumer<MagicStorageConfig, Integer> setter;

    Setting(String key, String shortName, boolean flag,
            Function<MagicStorageConfig, String> getter, BiConsumer<MagicStorageConfig, Integer> setter) {
        this.key = key;
        this.shortName = shortName;
        this.flag = flag;
        this.getter = getter;
        this.setter = setter;
    }

    @Nonnull
    String get(@Nonnull MagicStorageConfig config) {
        return getter.apply(config);
    }

    /** Parses the value as typed by a player, or returns null if it does not fit this setting. */
    @Nullable
    Integer parse(@Nonnull String input) {
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (flag) {
            return switch (value) {
                case "true", "on", "yes", "1", "an", "ja" -> 1;
                case "false", "off", "no", "0", "aus", "nein" -> 0;
                default -> null;
            };
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void set(@Nonnull MagicStorageConfig config, int value) {
        setter.accept(config, value);
    }

    @Nullable
    static Setting byName(@Nonnull String name) {
        for (Setting setting : values()) {
            if (setting.key.equalsIgnoreCase(name) || setting.shortName.equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    @Nonnull
    static String names() {
        return Arrays.stream(values()).map(s -> s.key + " (" + s.shortName + ")").collect(Collectors.joining(", "));
    }
}
