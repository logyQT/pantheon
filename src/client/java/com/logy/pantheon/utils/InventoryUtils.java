package com.logy.pantheon.utils;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryUtils {

    public static int getItemCountById(String itemID) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;

        Inventory inventory = client.player.getInventory();

        var optional = BuiltInRegistries.ITEM.get(Identifier.parse(itemID));
        if (optional.isEmpty()) return 0;

        Item targetItem = optional.get().value();

        int total = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static int getItemCountByName(String itemName){
        return 0;
    }

    public static boolean hasEnoughById(String itemID, int requiredAmount) {
        return getItemCountById(itemID) >= requiredAmount;
    }

    public static boolean hasEnoughByName(String itemName, int requiredAmount){
        return getItemCountByName(itemName) >= requiredAmount;
    }
}
