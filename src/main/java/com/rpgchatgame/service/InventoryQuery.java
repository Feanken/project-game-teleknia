package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

public final class InventoryQuery {
    public enum Sort { NAME, RARITY, QUANTITY }
    private InventoryQuery() {}
    public static List<InventoryItem> select(List<InventoryItem> items,Item.ItemType type,Sort sort){
        Comparator<InventoryItem> name=Comparator.comparing(i->i.getItem().getName(),String.CASE_INSENSITIVE_ORDER);
        Comparator<InventoryItem> comparator=switch(sort){
            case RARITY->Comparator.<InventoryItem>comparingInt(i->i.getItem().getRarity().ordinal()).reversed().thenComparing(name);
            case QUANTITY->Comparator.comparingInt(InventoryItem::getQuantity).reversed().thenComparing(name);
            default->name;
        };
        return items.stream().filter(i->type==null||i.getItem().getType()==type).sorted(comparator).toList();
    }
    public static String comparison(GameState state,String id){
        var entry=ItemCatalog.entry(id);if(entry==null)return "";
        if(entry.item().getType()==Item.ItemType.WEAPON){int before=ItemCatalog.attack(state);int after=state.getCharacter().getStrength()+entry.attack();return "Attack: "+before+" -> "+after+" ("+signed(after-before)+")";}
        if(entry.item().getType()==Item.ItemType.ARMOR){int before=ItemCatalog.defense(state);int after=state.getCharacter().getDefense()+entry.defense();return "Defense: "+before+" -> "+after+" ("+signed(after-before)+")";}
        return "";
    }
    private static String signed(int value){return value>0?"+"+value:Integer.toString(value);}
}
