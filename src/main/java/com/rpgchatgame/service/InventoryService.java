package com.rpgchatgame.service;

import com.rpgchatgame.model.Character;
import com.rpgchatgame.model.InventoryItem;
import com.rpgchatgame.model.Item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InventoryService {
    public List<InventoryItem> createStarterInventory() {
        List<InventoryItem> inventory = new ArrayList<>();
        inventory.add(new InventoryItem(new Item(
                "rusty_sword",
                "Rusty Sword",
                Item.ItemType.WEAPON,
                Item.Rarity.COMMON,
                "A worn blade with a chipped edge.",
                "+2 Attack flavor item"
        ), 1));
        inventory.add(new InventoryItem(new Item(
                "small_potion",
                "Small Potion",
                Item.ItemType.CONSUMABLE,
                Item.Rarity.COMMON,
                "A red potion that restores a modest amount of health.",
                "Restore 25 HP"
        ), 2));
        inventory.add(new InventoryItem(new Item(
                "old_map",
                "Old Map",
                Item.ItemType.QUEST,
                Item.Rarity.UNCOMMON,
                "A brittle map of the village and northern woods.",
                "Reveals hidden paths"
        ), 1));
        return inventory;
    }

    public void addItem(List<InventoryItem> inventory, InventoryItem gainedItem) {
        for (InventoryItem inventoryItem : inventory) {
            if (inventoryItem.getItem().getId().equals(gainedItem.getItem().getId())) {
                inventoryItem.setQuantity(inventoryItem.getQuantity() + gainedItem.getQuantity());
                return;
            }
        }
        inventory.add(gainedItem);
    }

    public String useItem(List<InventoryItem> inventory, Character character, String itemId) {
        Iterator<InventoryItem> iterator = inventory.iterator();
        while (iterator.hasNext()) {
            InventoryItem inventoryItem = iterator.next();
            Item item = inventoryItem.getItem();
            if (!item.getId().equals(itemId)) {
                continue;
            }
            if (item.getType() != Item.ItemType.CONSUMABLE) {
                return item.getName() + " cannot be used right now.";
            }
            if ("small_potion".equals(item.getId())) {
                int before = character.getHp();
                character.setHp(Math.min(character.getMaxHp(), character.getHp() + 25));
                inventoryItem.setQuantity(inventoryItem.getQuantity() - 1);
                if (inventoryItem.getQuantity() <= 0) {
                    iterator.remove();
                }
                return "Small Potion restored " + (character.getHp() - before) + " HP.";
            }
            return "Nothing happens.";
        }
        return "Item not found.";
    }
}
