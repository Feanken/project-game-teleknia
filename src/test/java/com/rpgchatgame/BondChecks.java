package com.rpgchatgame;

import com.rpgchatgame.service.*;
import com.rpgchatgame.model.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class BondChecks {
    static int count;
    static void check(boolean value,String text){count++;if(!value)throw new AssertionError(text);}
    static void act(GameService g,String id){g.performAction(id);}
    static void fight(GameService g){int n=0;while(g.getCurrentState().getCampaign().getEnemy()!=null&&n++<90){
        var s=g.getCurrentState();String move=s.getCharacter().getMp()>=6?"skill":"guard";
        if(s.getCharacter().getHp()<s.getCharacter().getMaxHp()/2&&s.getCharacter().getMp()>=8&&CombatService.cooldown(s,"heal")==0)move="heal";
        act(g,move);
    }check(n<90,"Encounter terminates");}
    static void progression()throws Exception{
        var g=WorldChecks.expansion("Mage","STANDARD",false,true,true);
        check(BondService.RECIPES.size()==8&&BondService.COMPANIONS.size()==3,"Release content counts");
        for(var c:BondService.COMPANIONS){
            g.companion("recruit",c.id());g.companion("activate",c.id());g.companion("talk",c.id());
            if(c.id().equals("mira"))act(g,"go_village");else if(c.id().equals("sera"))act(g,"go_frostveil");else{act(g,"go_trail");act(g,"go_shrine");}
            act(g,"journey_"+c.id());act(g,"go_camp");g.companion("bond",c.id());
            check(g.getCurrentState().getCampaign().has("bond_"+c.id()),"Personal quest resolved: "+c.id());
            int affinity=BondService.affinity(g.getCurrentState(),c);g.companion("talk",c.id());g.companion("talk",c.id());
            check(BondService.affinity(g.getCurrentState(),c)==affinity,"Dialogue cannot farm affinity");
            g.saveSlot(2);g.loadSlot(2);check(g.getCurrentState().getCampaign().getCompanion().equals(c.id()),"Active companion survives load");
        }
        act(g,"go_ruins");act(g,"archive");act(g,"go_tower");act(g,"fight_warden");fight(g);check(g.getCurrentState().getCampaign().has("warden"),"Main boss defeated with bonds active");
        act(g,"end_seal");String ending=g.getCurrentState().getCampaign().getEnding();check(!ending.isBlank(),"Real campaign ending");
        check(g.gallery().contains("Ending / "+ending),"Ending archived on commit");act(g,"explore_borderlands");act(g,"go_camp");
        check(BondService.camp(g.getCurrentState()),"Return to camp for endgame systems");
        g.getCurrentState().getCharacter().setGold(1000); // Fixture funding; all blueprint/material gates earned above.
        for(var r:BondService.RECIPES){int before=BondService.quantity(g.getCurrentState(),r.material()),gold=g.getCurrentState().getCharacter().getGold();
            g.craft(r.id());check(ItemCatalog.owns(g.getCurrentState(),r.output()),"Recipe obtained: "+r.id());
            check(BondService.quantity(g.getCurrentState(),r.material())==before-r.amount()&&g.getCurrentState().getCharacter().getGold()==gold-r.gold(),"Exact craft cost: "+r.id());
            if(ItemCatalog.entry(r.output()).item().getType()!=Item.ItemType.CONSUMABLE){g.craft(r.id());check(BondService.quantity(g.getCurrentState(),r.material())==before-r.amount(),"No duplicate equipment/cost");}
        }
        check(g.gallery().contains("Achievement / Master Artisan"),"All recipes profile achievement");
        g.companion("activate","sera");g.equipItem("awakened_frost");g.equipItem("woven_ward");g.equipItem("bond_charm");
        for(var c:BondService.CHALLENGES){
            g.getCurrentState().getCharacter().setHp(g.getCurrentState().getCharacter().getMaxHp());g.getCurrentState().getCharacter().setMp(g.getCurrentState().getCharacter().getMaxMp());
            var flags=new HashSet<>(g.getCurrentState().getCampaign().getFlags());g.challenge(c.id());g.saveSlot(3);g.loadSlot(3);
            check(g.getCurrentState().getCampaign().getChallenge().equals(c.id()),"Arena state survives load");
            String ally=g.getCurrentState().getCampaign().getCompanion();g.companion("activate","mira");check(g.getCurrentState().getCampaign().getCompanion().equals(ally),"No mid-battle switching");
            act(g,"companion");int cd=CombatService.cooldown(g.getCurrentState(),"companion"),turn=g.getCurrentState().getCampaign().getTurn();act(g,"companion");
            check(CombatService.cooldown(g.getCurrentState(),"companion")==cd&&g.getCurrentState().getCampaign().getTurn()==turn,"Support cooldown blocks without spending turn");
            fight(g);check(g.getCurrentState().getCampaign().has("challenge_"+c.id()),"Challenge clear: "+c.id());
            check(g.getCurrentState().getCampaign().getFlags().containsAll(flags)&&g.getCurrentState().getCampaign().getEnding().equals(ending),"Arena preserves all story flags/ending");
            int mats=g.getCurrentState().getInventory().stream().mapToInt(InventoryItem::getQuantity).sum();
            g.getCurrentState().getCharacter().setHp(g.getCurrentState().getCharacter().getMaxHp());g.getCurrentState().getCharacter().setMp(g.getCurrentState().getCharacter().getMaxMp());
            g.challenge(c.id());fight(g);check(g.getCurrentState().getInventory().stream().mapToInt(InventoryItem::getQuantity).sum()==mats,"Arena replay cannot farm items");
        }
        check(g.gallery().contains("Achievement / Arena Champion"),"Arena profile achievement");
        g.challenge("ash");act(g,"flee");check(g.getCurrentState().getCampaign().getChallenge().isBlank()&&g.getCurrentState().getCampaign().getCompanion().equals("sera"),"Retreat clears arena, retains companion");
        var profile=g.gallery();g.startNewStory("Next Hero","Rogue");check(g.gallery().containsAll(profile)&&g.getCurrentState().getCampaign().getCompanion().isBlank(),"Gallery survives new campaign; companion does not leak");
    }
    static void integrity()throws Exception{
        var db=WorldChecks.database();var g=new GameService(db);g.startNewStory();
        g.companion("recruit","mira");check(!g.getCurrentState().getCampaign().has("recruit_mira"),"Recruitment requires gate and camp");
        g.craft("potion");check(!g.getCurrentState().getCampaign().has("craft_potion"),"Crafting requires camp");
        var s=g.getCurrentState();s.setCurrentLocation("Survivor Camp");s.getCampaign().getFlags().addAll(List.of("survivors","wolf"));act(g,"look");
        g.companion("recruit","mira");g.companion("activate","mira");g.companion("bond","mira");check(!g.getCurrentState().getCampaign().has("bond_mira"),"Affinity gate enforced");
        int materials=BondService.quantity(g.getCurrentState(),"ember_shard");BondService.prepare(g.getCurrentState());check(BondService.quantity(g.getCurrentState(),"ember_shard")==materials,"Milestone rewards awarded once");
        g.getCurrentState().getCharacter().setGold(0);g.craft("potion");check(BondService.quantity(g.getCurrentState(),"ember_shard")==materials,"Failed craft preserves materials");
        g.getCurrentState().getCharacter().setGold(100);act(g,"look");String before=JsonUtil.toJson(g.getCurrentState());
        try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("CREATE TRIGGER fail_bond BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'blocked'); END");}
        try{g.craft("potion");throw new AssertionError("Save should fail");}catch(SaveGameRepository.SaveProblem expected){count++;}
        check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Craft transaction rolls back inventory/gold/flags");
        try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("DROP TRIGGER fail_bond");}
        @SuppressWarnings("unchecked") Map<String,Object> payload=JsonUtil.fromJson(JsonUtil.toJson(g.getCurrentState()),Map.class);
        @SuppressWarnings("unchecked") Map<String,Object> world=(Map<String,Object>)payload.get("campaign");world.remove("companion");world.remove("challenge");
        String json=JsonUtil.toJson(payload),checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));
        try(var c=db.getConnection();var q=c.prepareStatement("UPDATE save_slots SET version=4,payload=?,checksum=? WHERE slot=0")){q.setString(1,json);q.setString(2,checksum);q.executeUpdate();}
        var migrated=new GameService(db);migrated.loadSlot(0);check(migrated.getCurrentState().getCampaign().getCompanion().isBlank()&&BondService.quantity(migrated.getCurrentState(),"ember_shard")==materials,"Schema 4 migrates without duplicate material rewards");
        migrated.getCurrentState().getCampaign().setEnemy(new Enemy("wolf","Training",1000,1,0));migrated.getCurrentState().getCampaign().setCompanion("mira");
        migrated.performAction("look");int hp=migrated.getCurrentState().getCharacter().getHp();act(migrated,"companion");check(migrated.getCurrentState().getCampaign().getTurn()==0&&migrated.getCurrentState().getCharacter().getHp()==hp,"Full HP Mira support does not spend turn");
        migrated.getCurrentState().getCharacter().setHp(1);migrated.getCurrentState().getCampaign().getEnemy().setAttack(1000);act(migrated,"attack");
        check(migrated.getCurrentState().getCampaign().getEnemy()==null&&migrated.getCurrentState().getCampaign().getCompanion().equals("mira"),"Defeat retains companion and clears cooldowns");
        migrated.getCurrentState().getCampaign().setEnding("LEGACY ENDING");migrated.saveSlot(1);
        try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("DELETE FROM profile_unlocks");}
        migrated.loadSlot(0);migrated.loadSlot(1);migrated.startNewStory();
        check(migrated.gallery().contains("Ending / LEGACY ENDING"),"Loading an old completed slot archives ending before New Game");
    }

    static void supportAndPotions()throws Exception{
        for(String klass:List.of("Fighter","Rogue","Mage"))for(var ally:BondService.COMPANIONS){
            var g=new GameService(WorldChecks.database());g.startNewStory("Ally test",klass);
            var w=g.getCurrentState().getCampaign();w.mark("recruit_"+ally.id(),"");w.setCompanion(ally.id());w.setEnemy(new Enemy("wolf","Training",1000,1,0));
            var hero=g.getCurrentState().getCharacter();hero.setHp(hero.getMaxHp()/2);int hp=hero.getHp();act(g,"look");act(g,"companion");
            check(g.getCurrentState().getCampaign().getTurn()==1&&CombatService.cooldown(g.getCurrentState(),"companion")==3,"Support spends turn with bounded cooldown: "+klass+"/"+ally.id());
            if(ally.id().equals("mira"))check(g.getCurrentState().getCharacter().getHp()==hp+7,"Mira heals eight, incoming one");
            if(ally.id().equals("sera"))check(g.getCurrentState().getCampaign().getEnemy().getHp()==992,"Sera deals eight damage");
            if(ally.id().equals("orin"))check(g.getCurrentState().getCharacter().getHp()==hp&&g.getCurrentState().getCampaign().getPlayerEffects().get("shield").getPotency()==7,"Orin absorbs direct damage");
            g.saveSlot(1);g.loadSlot(1);check(CombatService.cooldown(g.getCurrentState(),"companion")==3,"Support cooldown persisted");
        }
        var g=new GameService(WorldChecks.database());g.startNewStory();var s=g.getCurrentState();s.getCampaign().setEnemy(new Enemy("wolf","Training",1000,1,0));
        new InventoryService().addItem(s.getInventory(),ItemCatalog.stack("greater_potion",1));new InventoryService().addItem(s.getInventory(),ItemCatalog.stack("focus_draught",1));
        s.getCharacter().setHp(1);s.getCharacter().setMp(0);act(g,"look");act(g,"greater");
        check(!ItemCatalog.owns(g.getCurrentState(),"greater_potion")&&g.getCurrentState().getCharacter().getHp()==65,"Crafted greater potion heals 65 and spends a battle turn");
        act(g,"focus");check(!ItemCatalog.owns(g.getCurrentState(),"focus_draught")&&g.getCurrentState().getCharacter().getMp()==Math.min(45,g.getCurrentState().getCharacter().getMaxMp()),"Crafted focus draught restores MP in battle");
        var prefs=new UiSettings(Files.createTempDirectory(Path.of("target"),"bond-settings-").resolve("ui.properties"));prefs.setReducedMotion(true);
        check(prefs.reducedMotion(),"Reduced motion preference supported");
    }
    public static void main(String[] args)throws Exception{progression();integrity();supportAndPotions();System.out.println("PASS / "+count+" bond checks: recruitment, affinity, personal quests, eight recipes, finite rewards, arena replay/load/retreat, gallery and rollback.");}
}
