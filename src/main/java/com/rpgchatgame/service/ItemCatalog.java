package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;
import static com.rpgchatgame.model.Item.ItemType.*;
import static com.rpgchatgame.model.Item.Rarity.*;

/** Gameplay values live here; descriptive item text never determines stats. */
public final class ItemCatalog {
    public record Entry(Item item, int price, int attack, int defense, int hp, int mp) {}
    private static final Map<String, Entry> ITEMS = new LinkedHashMap<>();
    static {
        add("rusty_sword", "Rusty Sword", WEAPON, COMMON, "A chipped blade carried from the village.", "+2 Attack", 0,2,0,0,0);
        add("small_potion", "Small Potion", CONSUMABLE, COMMON, "A flask of red mountain herbs.", "Restore 35 HP", 12,0,0,35,0);
        add("mana_potion", "Mana Draught", CONSUMABLE, UNCOMMON, "Cool water steeped in moonroot.", "Restore 25 MP", 15,0,0,0,25);
        add("old_map", "Old Map", QUEST, UNCOMMON, "A hunter's map, with a shrine circled in faded ink.", "Reveals the Emberwood route",0,0,0,0,0);
        add("ash_fang", "Ash Fang", QUEST, UNCOMMON, "A crack of red light runs through the wolf's fang.", "Evidence of the Cinder Crown",0,0,0,0,0);
        add("iron_sword", "Iron Longsword", WEAPON, UNCOMMON, "A balanced blade forged by the camp smith.", "+8 Attack",40,8,0,0,0);
        add("ranger_armor", "Ranger Armor", ARMOR, UNCOMMON, "Layered leather reinforced with iron scales.", "+5 Defense",35,0,5,0,0);
        add("ember_blade", "Ember Blade", WEAPON, EPIC, "The guardian's gift, tempered by a quiet flame.", "+12 Attack",0,12,0,0,0);
        add("silver_locket", "Silver Locket", QUEST, RARE, "A portrait of Mira's brother, rescued from the ruins.", "Return to Mira at camp",0,0,0,0,0);
        add("sun_sigil", "Sun Sigil", QUEST, RARE, "The shrine's last unbroken seal.", "Needed to purify the Crown",0,0,0,0,0);
    }
    private ItemCatalog() {}
    private static void add(String id,String name,Item.ItemType type,Item.Rarity rarity,String description,String effect,
                            int price,int attack,int defense,int hp,int mp) {
        ITEMS.put(id,new Entry(new Item(id,name,type,rarity,description,effect),price,attack,defense,hp,mp));
    }
    public static Entry entry(String id) { return ITEMS.get(id); }
    public static InventoryItem stack(String id,int quantity) {
        Entry entry=Objects.requireNonNull(ITEMS.get(id), "Unknown catalog item: "+id);
        Item i=entry.item();
        return new InventoryItem(new Item(i.getId(),i.getName(),i.getType(),i.getRarity(),i.getDescription(),i.getEffect()),quantity);
    }
    public static List<Entry> shop() { return List.of(ITEMS.get("small_potion"),ITEMS.get("mana_potion"),ITEMS.get("iron_sword"),ITEMS.get("ranger_armor")); }
    public static boolean owns(GameState state,String id) { return state.getInventory().stream().anyMatch(i->i.getItem().getId().equals(id)&&i.getQuantity()>0); }
    public static int attack(GameState state) {
        Entry weapon=entry(state.getCampaign().getWeapon());
        return state.getCharacter().getStrength()+(weapon!=null&&owns(state,weapon.item().getId())?weapon.attack():0);
    }
    public static int defense(GameState state) {
        Entry armor=entry(state.getCampaign().getArmor());
        return state.getCharacter().getDefense()+(armor!=null&&owns(state,armor.item().getId())?armor.defense():0);
    }
    public static String equippedName(String id) { return entry(id)==null?"None":entry(id).item().getName(); }
}
