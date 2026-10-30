package com.rpgchatgame;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;

/** Authoritative map and inventory rules, with isolated SQLite transaction failures. */
public final class AtlasChecks {
    static int count;
    static void check(boolean ok,String label){count++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        var db=WorldChecks.database();var g=new GameService(db);g.startNewStory();
        var nodes=AtlasService.nodes(g.getCurrentState());check(nodes.stream().filter(AtlasService.Node::known).count()==1,"Only starting location known");
        check(nodes.stream().noneMatch(n->n.id().equals("tower")),"Remote locations absent from fog graph");
        check(nodes.stream().anyMatch(n->n.id().equals("gate")&&!n.known()&&n.reachable()),"Unknown frontier can be explored");
        check(nodes.stream().anyMatch(n->n.id().equals("camp")&&!n.reachable()),"Quest-locked frontier cannot travel");
        String before=JsonUtil.toJson(g.getCurrentState());try{g.travel("tower");throw new AssertionError("Teleport allowed");}catch(IllegalStateException expected){count++;}
        check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Rejected travel preserves state");g.travel("gate");g.saveSlot(1);g.loadSlot(1);
        check(AtlasService.nodes(g.getCurrentState()).stream().anyMatch(n->n.id().equals("gate")&&n.known()&&n.current()),"Explored location persists");
        g.performAction("fight_wolf");check(!AtlasService.canTravel(g.getCurrentState(),"village"),"No travel during combat");
        g.performAction("flee");g.getCurrentState().getCampaign().mark("survivors","");g.performAction("look");g.performAction("npc_mira");check(!AtlasService.canTravel(g.getCurrentState(),"village"),"No travel during conversation");g.performAction("dialogue_close");
        for(String klass:List.of("Fighter","Rogue","Mage")){
            g.startNewStory("Equip test",klass);int hp=g.getCurrentState().getCharacter().getHp(),mp=g.getCurrentState().getCharacter().getMp();
            g.unequipItem("rusty_sword");check(g.getCurrentState().getCampaign().getWeapon().isBlank(),"Weapon can be empty");check(ItemCatalog.attack(g.getCurrentState())==g.getCurrentState().getCharacter().getStrength(),"Unequipped base attack");
            check(g.getCurrentState().getCharacter().getHp()==hp&&g.getCurrentState().getCharacter().getMp()==mp,"Unequip never heals");
            g.saveSlot(1);g.loadSlot(1);check(g.getCurrentState().getCampaign().getWeapon().isBlank(),"Empty equipment survives load");
            g.equipItem("rusty_sword");g.removeItem("rusty_sword",1);check(ItemCatalog.owns(g.getCurrentState(),"rusty_sword"),"Equipped equipment protected");
            g.unequipItem("rusty_sword");g.removeItem("rusty_sword",1);check(!ItemCatalog.owns(g.getCurrentState(),"rusty_sword"),"Unequipped equipment can be discarded");
        }
        g.startNewStory();int gold=g.getCurrentState().getCharacter().getGold();g.removeItem("old_map",1);check(ItemCatalog.owns(g.getCurrentState(),"old_map"),"Quest items protected");
        new InventoryService().addItem(g.getCurrentState().getInventory(),ItemCatalog.stack("ember_shard",3));g.removeItem("ember_shard",3);check(BondService.quantity(g.getCurrentState(),"ember_shard")==3,"Finite crafting material protected");
        int potions=BondService.quantity(g.getCurrentState(),"small_potion");g.removeItem("small_potion",-1);g.removeItem("small_potion",999);check(BondService.quantity(g.getCurrentState(),"small_potion")==potions,"Invalid quantities rejected");
        g.removeItem("small_potion",2);check(BondService.quantity(g.getCurrentState(),"small_potion")==potions-2&&g.getCurrentState().getCharacter().getGold()==gold,"Partial discard consumes exact stack without gold");
        g.travel("gate");g.performAction("fight_wolf");before=JsonUtil.toJson(g.getCurrentState());g.unequipItem("rusty_sword");g.removeItem("small_potion",1);check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Inventory operations blocked in combat without save side effects");
        g.performAction("flee");g.getCurrentState().getCharacter().setGold(200);g.performAction("look");int price=WorldService.price(g.getCurrentState(),ItemCatalog.entry("mana_potion"));int qty=BondService.quantity(g.getCurrentState(),"mana_potion");g.buyItems("mana_potion",3);
        check(g.getCurrentState().getCharacter().getGold()==200-price*3&&BondService.quantity(g.getCurrentState(),"mana_potion")==qty+3,"Bulk purchase charges one atomic exact total");
        gold=g.getCurrentState().getCharacter().getGold();g.buyItems("mana_potion",Integer.MAX_VALUE);g.buyItems("iron_sword",2);check(g.getCurrentState().getCharacter().getGold()==gold,"Invalid bulk purchases rejected");
        g.getCurrentState().getCampaign().setEnding("LEGACY ENDING");g.performAction("look");before=JsonUtil.toJson(g.getCurrentState());g.removeItem("small_potion",1);g.unequipItem("rusty_sword");check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Closed epilogue locks destructive actions");g.performAction("explore_borderlands");
        for(String operation:List.of("remove","unequip","buy","travel")){
            g.equipItem("rusty_sword");g.performAction("look");String original=JsonUtil.toJson(g.getCurrentState());
            try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("CREATE TRIGGER fail_atlas BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'atlas rollback'); END");}
            try{switch(operation){case "remove"->g.removeItem("mana_potion",1);case "unequip"->g.unequipItem("rusty_sword");case "buy"->g.buyItems("small_potion",1);case "travel"->g.travel("village");}throw new AssertionError("Expected transaction failure");}catch(SaveGameRepository.SaveProblem expected){count++;}
            check(JsonUtil.toJson(g.getCurrentState()).equals(original),"Failed "+operation+" preserves inventory/equipment/progression");
            try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("DROP TRIGGER fail_atlas");}
        }
        System.out.println("PASS / "+count+" atlas checks: fog, frontier, route gates, empty equipment, discard, bulk trade, save/load and rollback.");
    }
}
