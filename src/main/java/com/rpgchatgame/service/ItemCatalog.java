package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** Gameplay values live here; descriptive item text never determines stats. */
public final class ItemCatalog {
    public record Entry(Item item, int price, int attack, int defense, int hp, int mp, boolean sold, int critical) {}
    private static final Map<String, Entry> ITEMS = new LinkedHashMap<>();
    static {
        try (var input=ItemCatalog.class.getResourceAsStream("/definitions/items.json")) {
            if(input==null)throw new IllegalStateException("Item definitions missing");
            var entries=com.rpgchatgame.util.JsonUtil.fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),Entry[].class);
            for(var entry:entries) {
                var item=entry.item();
                if(item==null||item.getId()==null||item.getId().isBlank()||item.getName()==null||item.getDescription()==null||item.getEffect()==null||item.getType()==null||item.getRarity()==null
                        ||entry.price()<0||(entry.sold()&&entry.price()==0)||entry.attack()<0||entry.defense()<0||entry.hp()<0||entry.mp()<0||entry.critical()<0||entry.critical()>1
                        ||ITEMS.putIfAbsent(item.getId(),entry)!=null)throw new IllegalStateException("Invalid or duplicate item definition");
            }
            for(String id:List.of("rusty_sword","small_potion","mana_potion","old_map","ash_fang","iron_sword","ranger_armor","ember_blade","silver_locket","sun_sigil"))
                if(!ITEMS.containsKey(id))throw new IllegalStateException("Missing campaign item: "+id);
        } catch(java.io.IOException error) { throw new ExceptionInInitializerError(error); }
    }
    private ItemCatalog() {}
    public static Entry entry(String id) { return ITEMS.get(id); }
    public static InventoryItem stack(String id,int quantity) {
        Entry entry=Objects.requireNonNull(ITEMS.get(id), "Unknown catalog item: "+id);
        Item i=entry.item();
        return new InventoryItem(new Item(i.getId(),i.getName(),i.getType(),i.getRarity(),i.getDescription(),i.getEffect()),quantity);
    }
    public static List<Entry> shop() { return ITEMS.values().stream().filter(Entry::sold).toList(); }
    public static boolean owns(GameState state,String id) { return state.getInventory().stream().anyMatch(i->i.getItem().getId().equals(id)&&i.getQuantity()>0); }
    public static int attack(GameState state) {
        Entry weapon=entry(state.getCampaign().getWeapon());
        return state.getCharacter().getStrength()+(weapon!=null&&owns(state,weapon.item().getId())?weapon.attack():0)+accessoryAttack(state);
    }
    public static int defense(GameState state) {
        Entry armor=entry(state.getCampaign().getArmor());
        return state.getCharacter().getDefense()+(armor!=null&&owns(state,armor.item().getId())?armor.defense():0)+accessoryDefense(state);
    }
    private static Entry accessory(GameState state){var item=entry(state.getCampaign().getAccessory());return item!=null&&owns(state,item.item().getId())?item:null;}
    public static int accessoryAttack(GameState state){var item=accessory(state);return item==null?0:item.attack();}
    public static int accessoryDefense(GameState state){var item=accessory(state);return item==null?0:item.defense();}
    public static int criticalPeriod(GameState state){var item=accessory(state);return item!=null&&item.critical()>0?3:4;}
    public static boolean criticalEligible(GameState state){return state.getCharacter().getAgility()>=10||criticalPeriod(state)==3;}
    public static String equippedName(String id) { return entry(id)==null?"None":entry(id).item().getName(); }
}
