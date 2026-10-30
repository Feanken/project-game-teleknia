package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** Fog is presentation only; authoritative travel always checks current routes. */
public final class AtlasService {
    public record Node(String id,boolean known,boolean current,boolean reachable) {}
    public static boolean canTravel(GameState s,String id){return WorldService.routes(s).stream().anyMatch(c->c.getAction().equals("go_"+id));}
    public static List<Node> nodes(GameState s){
        var w=s.getCampaign();String current=WorldContent.location(s.getCurrentLocation()).id();
        Set<String> visible=new LinkedHashSet<>();visible.add(current);
        for(var l:WorldContent.LOCATIONS.values())if(w.getVisited().contains(l.name()))visible.add(l.id());
        // Frontier nodes reveal only their existence, never their name, art or onward routes.
        for(String id:new ArrayList<>(visible))for(var r:WorldContent.LOCATIONS.get(id).routes())visible.add(r.to());
        return WorldContent.LOCATIONS.values().stream().filter(l->visible.contains(l.id())).map(l->new Node(l.id(),w.getVisited().contains(l.name())||l.id().equals(current),l.id().equals(current),canTravel(s,l.id()))).toList();
    }
    public static boolean equipped(GameState s,String id){var w=s.getCampaign();return id!=null&&!id.isBlank()&&(id.equals(w.getWeapon())||id.equals(w.getArmor())||id.equals(w.getAccessory()));}
    public static String inventoryLock(GameState s){return s.getCampaign().getEnemy()!=null?"Finish combat before changing your inventory.":s.getCampaign().epilogueClosed()?"Reopen the borderlands before changing your inventory.":"";}
    public static String removeBlock(GameState s,String id){
        String lock=inventoryLock(s);if(!lock.isBlank())return lock;
        var item=ItemCatalog.entry(id);if(item==null||!ItemCatalog.owns(s,id))return "Item not found.";
        if(item.item().getType()==Item.ItemType.QUEST)return "Quest items and crafting materials cannot be removed.";
        if(equipped(s,id))return "Unequip this item before removing it.";
        return "";
    }
}
