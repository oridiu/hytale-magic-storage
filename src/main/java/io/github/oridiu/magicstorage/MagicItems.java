package io.github.oridiu.magicstorage;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nullable;

/** Item ids of the mod, as named by the JSON files under Server/Item/Items. */
public final class MagicItems {

    public static final String CHEST_ID = "Magic_Chest";
    public static final String BAG_ID = "Magic_Bag";

    private MagicItems() {
    }

    public static boolean isBag(@Nullable ItemStack stack) {
        return !ItemStack.isEmpty(stack) && BAG_ID.equals(stack.getItemId());
    }

    /** True for the chest block in any of its states (open, closed). */
    public static boolean isChest(@Nullable BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        Item item = blockType.getItem();
        return item != null && CHEST_ID.equals(item.getId());
    }
}
