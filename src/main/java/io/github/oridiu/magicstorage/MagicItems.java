package io.github.oridiu.magicstorage;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nullable;

/** Item ids of the mod, as named by the JSON files under Server/Item/Items. */
public final class MagicItems {

    public static final String CHEST_ID = "Magic_Chest";
    /** Two Magic Chests side by side turn into this block, like vanilla chests. */
    public static final String LARGE_CHEST_ID = "Magic_Chest_Large";
    public static final String BAG_ID = "Magic_Bag";

    private MagicItems() {
    }

    public static boolean isBag(@Nullable ItemStack stack) {
        return !ItemStack.isEmpty(stack) && BAG_ID.equals(stack.getItemId());
    }

    /** True for a small or large Magic Chest in any of its states (open, closed). */
    public static boolean isChest(@Nullable BlockType blockType) {
        String id = itemId(blockType);
        return CHEST_ID.equals(id) || LARGE_CHEST_ID.equals(id);
    }

    public static boolean isLargeChest(@Nullable BlockType blockType) {
        return LARGE_CHEST_ID.equals(itemId(blockType));
    }

    @Nullable
    private static String itemId(@Nullable BlockType blockType) {
        if (blockType == null) {
            return null;
        }
        Item item = blockType.getItem();
        return item != null ? item.getId() : null;
    }
}
