package com.rpgchatgame.service;

import com.rpgchatgame.model.Character;
import com.rpgchatgame.model.*;
import java.util.*;

public class InventoryService {
    public List<InventoryItem> createStarterInventory() {
        return new ArrayList<>(List.of(ItemCatalog.stack("rusty_sword",1),ItemCatalog.stack("small_potion",3),
                ItemCatalog.stack("mana_potion",1),ItemCatalog.stack("old_map",1)));
    }
    public void addItem(List<InventoryItem> inventory,InventoryItem gained) {
        if (gained.getQuantity()<=0) throw new IllegalArgumentException("Quantity must be positive");
        for (InventoryItem item:inventory) if (item.getItem().getId().equals(gained.getItem().getId())) {
            item.setQuantity(item.getQuantity()+gained.getQuantity()); return;
        }
        inventory.add(gained);
    }
    public boolean remove(List<InventoryItem> inventory,String id) {
        for (var iterator=inventory.iterator();iterator.hasNext();) {
            InventoryItem item=iterator.next();
            if(item.getItem().getId().equals(id)&&item.getQuantity()>0) {
                item.setQuantity(item.getQuantity()-1);if(item.getQuantity()==0)iterator.remove();return true;
            }
        }
        return false;
    }
    public String useItem(List<InventoryItem> inventory,Character character,String id) {
        var entry=ItemCatalog.entry(id);
        if(entry==null||entry.item().getType()!=Item.ItemType.CONSUMABLE) return "This item cannot be consumed.";
        if(entry.hp()>0&&character.getHp()>=character.getMaxHp()) return "HP is already full. Potion kept.";
        if(entry.mp()>0&&character.getMp()>=character.getMaxMp()) return "MP is already full. Draught kept.";
        if(!remove(inventory,id))return "Item not found.";
        int hp=character.getHp(),mp=character.getMp();
        character.setHp(Math.min(character.getMaxHp(),hp+entry.hp()));
        character.setMp(Math.min(character.getMaxMp(),mp+entry.mp()));
        return entry.item().getName()+" restored "+(entry.hp()>0?(character.getHp()-hp)+" HP.":(character.getMp()-mp)+" MP.");
    }
}
