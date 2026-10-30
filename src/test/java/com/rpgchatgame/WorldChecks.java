package com.rpgchatgame;

import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.*;
import java.nio.file.*;
import java.util.*;
import java.sql.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** 1.4 content, reputation, branching, data/asset integrity, and save compatibility. */
public final class WorldChecks {
    static int assertions;
    static void check(boolean value,String text){assertions++;if(!value)throw new AssertionError(text);}
    static DatabaseManager database()throws Exception{Path root=Path.of("target","world-checks");Files.createDirectories(root);return new DatabaseManager(Files.createTempDirectory(root,"case-").resolve("save.db"));}
    static void act(GameService g,String id){g.performAction(id);}
    static void leave(GameService g){if(!g.getCurrentState().getCampaign().getDialogueNode().isBlank())act(g,"dialogue_close");}
    static void battle(GameService g,String flag){int n=0;while(g.getCurrentState().getCampaign().getEnemy()!=null&&n++<80){var s=g.getCurrentState();String move="skill";
        if(s.getCharacter().getHp()<s.getCharacter().getMaxHp()/3&&ItemCatalog.owns(s,"small_potion"))move="potion";
        else if(s.getCharacter().getHp()<s.getCharacter().getMaxHp()/3&&s.getCharacter().getMp()>=8&&CombatService.cooldown(s,"heal")==0)move="heal";
        else if(CombatService.intent(s).bonus()>0||s.getCharacter().getMp()<6)move="guard";act(g,move);
    }check(n<80&&g.getCurrentState().getCampaign().has(flag),"Encounter completed: "+flag);}
    static GameService setup(String klass,String difficulty)throws Exception{
        var g=new GameService(database());g.startNewStory("Border "+klass,klass,difficulty);g.upgradeSkill("skill");act(g,"search");act(g,"rescue");act(g,"go_gate");act(g,"fight_wolf");battle(g,"wolf");act(g,"go_camp");
        g.buyItem("iron_sword");g.buyItem("ranger_armor");g.equipItem("iron_sword");g.equipItem("ranger_armor");act(g,"rest");act(g,"talk_mira");act(g,"go_trail");act(g,"herbs");act(g,"go_shrine");act(g,"runes");act(g,"fight_guardian");battle(g,"guardian");g.equipItem("ember_blade");act(g,"sigil");act(g,"go_camp");return g;
    }
    static GameService expansion(String klass,String difficulty,boolean peaceful,boolean returnCrate,boolean keeperChart)throws Exception{
        var g=setup(klass,difficulty);act(g,"go_frostveil");act(g,"find_crate");act(g,"find_tablet");act(g,"go_glacier");act(g,"find_lens");act(g,"go_frostveil");act(g,"npc_sera");act(g,"repair_signal");act(g,returnCrate?"return_crate":"keep_crate");
        int gold=g.getCurrentState().getCharacter().getGold();act(g,returnCrate?"keep_crate":"return_crate");check(g.getCurrentState().getCharacter().getGold()==gold,"Caravan branches mutually exclusive");leave(g);
        act(g,"go_glacier");act(g,"npc_orin");act(g,"study_oath");
        if(peaceful){act(g,"parley_rime");check(g.getCurrentState().getCampaign().has("rime_peace")&&!g.getCurrentState().getCampaign().has("rime_sentinel"),"Keeper reputation opens noncombat sentinel resolution");leave(g);}
        else{act(g,"fight_rime");battle(g,"rime_sentinel");check(ItemCatalog.owns(g.getCurrentState(),"frost_blade"),"Sentinel combat loot");}
        check(g.getCurrentState().getCampaign().has("q_oath"),"Oath quest resolved either way");
        act(g,"go_frostveil");act(g,"go_camp");act(g,"rest");act(g,"go_coast");act(g,"find_medicine");act(g,"find_chart");act(g,"find_bell");act(g,"npc_marin");act(g,"ask_ledger");leave(g);
        act(g,"go_camp");act(g,"npc_mira");act(g,"deliver_medicine");leave(g);act(g,"rest");act(g,"go_coast");act(g,"go_lighthouse");act(g,"npc_ilyra");act(g,keeperChart?"chart_keepers":"chart_camp");
        gold=g.getCurrentState().getCharacter().getGold();int reputation=WorldService.reputation(g.getCurrentState(),keeperChart?"camp":"keepers");act(g,keeperChart?"chart_camp":"chart_keepers");
        check(g.getCurrentState().getCharacter().getGold()==gold&&WorldService.reputation(g.getCurrentState(),keeperChart?"camp":"keepers")==reputation,"Chart branches mutually exclusive");
        if(peaceful){act(g,"parley_captain");check(g.getCurrentState().getCampaign().has("captain_peace")&&!g.getCurrentState().getCampaign().has("drowned_captain"),"Camp reputation opens noncombat captain resolution");leave(g);}
        else{act(g,"fight_captain");battle(g,"drowned_captain");}
        for(String quest:List.of("q_signal","q_caravan","q_oath","q_medicine","q_chart","q_captain"))check(g.getCurrentState().getCampaign().has(quest),"Side quest complete: "+quest);
        check(ItemCatalog.owns(g.getCurrentState(),"tide_ring")&&ItemCatalog.owns(g.getCurrentState(),"rime_sigil"),"Both encounter rewards available through either route");
        check(g.getCurrentState().getCampaign().getCodex().containsAll(List.of("npc:sera","npc:orin","npc:marin","npc:ilyra","enemy:rime_sentinel","enemy:drowned_captain")),"Discoveries reflect conversations and peaceful resolutions");
        act(g,"go_coast");act(g,"go_camp");int price=WorldService.price(g.getCurrentState(),ItemCatalog.entry("small_potion"));
        check(price==9,"Trusted camp grants real 20% discount");gold=g.getCurrentState().getCharacter().getGold();g.buyItem("small_potion");check(g.getCurrentState().getCharacter().getGold()==gold-price,"Discount is charged transactionally");
        return g;
    }
    static void gatesAndReplay()throws Exception{
        var db=database();var g=new GameService(db);g.startNewStory();act(g,"go_frostveil");check(g.getCurrentState().getCurrentLocation().equals("Burned Village"),"No teleport to locked expansion");
        g.getCurrentState().getCampaign().getFlags().addAll(List.of("wolf","guardian"));act(g,"go_gate");act(g,"go_frostveil");act(g,"npc_sera");
        int rep=WorldService.reputation(g.getCurrentState(),"camp");
        int xp=g.getCurrentState().getCharacter().getExp();act(g,"ask_signal");check(g.getCurrentState().getCampaign().getDialogueNode().equals("sera_signal"),"NPC questions branch to data-driven dialogue nodes");
        act(g,"back_sera");check(g.getCurrentState().getCampaign().getDialogueNode().equals("sera"),"Dialogue can return to its questions");
        check(g.getCurrentState().getCharacter().getExp()==xp&&WorldService.reputation(g.getCurrentState(),"camp")==rep,"Hints cannot farm rewards");act(g,"repair_signal");check(!g.getCurrentState().getCampaign().has("q_signal"),"Quest item required");
        leave(g);act(g,"npc_sera");leave(g);check(WorldService.reputation(g.getCurrentState(),"camp")==rep,"Dialogue introductions cannot farm reputation");
        act(g,"find_crate");act(g,"npc_sera");g.saveSlot(2);var loaded=new GameService(db);loaded.loadSlot(2);
        check(loaded.getCurrentState().getCampaign().getDialogueNode().equals("sera")&&loaded.getCurrentState().getCurrentChoices().stream().anyMatch(c->c.getAction().equals("return_crate")),"Active dialogue and replies survive load");
        String before=JsonUtil.toJson(loaded.getCurrentState());
        try(var c=db.getConnection();var s=c.createStatement()){s.executeUpdate("CREATE TRIGGER fail_quest BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'failed reward'); END");}
        try{act(loaded,"return_crate");throw new AssertionError("Commit must fail");}catch(IllegalStateException expected){assertions++;}
        check(JsonUtil.toJson(loaded.getCurrentState()).equals(before),"Failed quest commit restores item, reputation, flags and rewards");
        var low=new GameService(database());low.startNewStory();low.getCurrentState().getCampaign().getFlags().addAll(List.of("wolf","guardian","q_chart","captain_ledger","oath_read"));
        new InventoryService().addItem(low.getCurrentState().getInventory(),ItemCatalog.stack("captains_bell",1));new InventoryService().addItem(low.getCurrentState().getInventory(),ItemCatalog.stack("rune_tablet",1));
        act(low,"go_gate");act(low,"go_frostveil");act(low,"go_glacier");act(low,"npc_orin");act(low,"parley_rime");check(!low.getCurrentState().getCampaign().has("rime_resolved"),"Keeper reputation gate enforced");leave(low);
        act(low,"go_frostveil");act(low,"go_camp");act(low,"go_coast");act(low,"go_lighthouse");act(low,"npc_ilyra");act(low,"parley_captain");check(!low.getCurrentState().getCampaign().has("captain_resolved"),"Camp reputation gate enforced");
    }
    @SuppressWarnings("unchecked") static void migrationAndEpilogue()throws Exception{
        var original=setup("Fighter","STANDARD");act(original,"go_ruins");act(original,"archive");act(original,"go_tower");act(original,"fight_warden");battle(original,"warden");act(original,"end_seal");String ending=original.getCurrentState().getCampaign().getEnding();
        Map<String,Object> payload=JsonUtil.fromJson(JsonUtil.toJson(original.getCurrentState()),Map.class);Map<String,Object> world=(Map<String,Object>)payload.get("campaign");
        for(String key:List.of("reputation","codex","dialogueNode","trackedQuest"))world.remove(key);((List<String>)world.get("flags")).remove("world14_initialized");
        String json=JsonUtil.toJson(payload),checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));var target=database();target.initialize();
        try(var c=target.getConnection();var s=c.prepareStatement("INSERT INTO save_slots(slot,version,saved_at,payload,checksum) VALUES(0,3,'2026-10-29T00:00:00Z',?,?)")){s.setString(1,json);s.setString(2,checksum);s.executeUpdate();}
        var migrated=new GameService(target);migrated.continueStory();check(migrated.getCurrentState().getCampaign().getEnding().equals(ending),"1.3 migration preserves ending");
        check(WorldService.reputation(migrated.getCurrentState(),"camp")==2&&WorldService.reputation(migrated.getCurrentState(),"keepers")==2,"Legacy milestones seed reputation once");
        act(migrated,"explore_borderlands");check(migrated.getCurrentState().getCurrentLocation().equals("Survivor Camp")&&migrated.canShop(),"Finished saves can explicitly resume optional exploration");
        act(migrated,"end_claim");check(migrated.getCurrentState().getCampaign().getEnding().equals(ending),"Expansion never rewrites ending");
        act(migrated,"go_frostveil");migrated.trackQuest("q_signal");migrated.saveSlot(3);
        var reread=new GameService(target);reread.loadSlot(3);check(reread.getCurrentState().getCampaign().getTrackedQuest().equals("q_signal")&&reread.getCurrentState().getCampaign().getEnding().equals(ending),"Tracked quest and epilogue persist");
    }
    static void assetsAndData()throws Exception{
        WorldContent.validate();check(WorldContent.LOCATIONS.size()==11&&WorldContent.NPCS.size()==5&&WorldContent.MONSTERS.size()==11&&WorldContent.QUESTS.size()==13,"World content counts");
        for(String item:List.of("frost_lens","rune_tablet","caravan_crate","tide_medicine","star_chart","captains_bell","frost_lantern","prospectors_blade","glacier_mail","frost_blade","rime_sigil","tide_ring"))check(ItemCatalog.entry(item)!=null,"New item definition: "+item);
        Set<String> sprites=new HashSet<>();WorldContent.LOCATIONS.values().forEach(l->sprites.add(l.sprite()));WorldContent.NPCS.values().forEach(n->sprites.add(n.sprite()));WorldContent.MONSTERS.values().forEach(e->sprites.add(e.sprite()));
        sprites.addAll(List.of("sera","orin","marin","ilyra","rime_sentinel","drowned_captain","frostveil","glacier","coast","lighthouse","frost_lens","rune_tablet","caravan_crate","tide_medicine","star_chart","captains_bell","field_fighter","field_rogue","field_mage","field_wolf","field_guardian","field_warden","field_trail","field_village","field_gate","field_camp","field_shrine","field_ruins","field_tower","frost_blade","glacier_mail","tide_ring"));
        for(String sprite:sprites){var url=WorldChecks.class.getResource("/sprites/"+sprite+".png");check(url!=null,"Bundled sprite: "+sprite);var image=javax.imageio.ImageIO.read(url);check(image.getWidth()==64&&image.getHeight()==64,"Native 64px sprite: "+sprite);}
        for(String name:List.of("click","action","damage","potion","reward","level_up","discovery","quest_complete"))try(var stream=javax.sound.sampled.AudioSystem.getAudioInputStream(WorldChecks.class.getResource("/audio/"+name+".wav"))){check(stream.getFrameLength()>0&&stream.getFormat().getSampleSizeInBits()==16,"Bundled offline audio: "+name);}
    }
    public static void main(String[] args)throws Exception{
        assetsAndData();gatesAndReplay();migrationAndEpilogue();
        for(String klass:List.of("Fighter","Rogue","Mage"))for(String difficulty:List.of("STANDARD","STORY")){
            var g=expansion(klass,difficulty,false,true,true);check(g.getCurrentState().getCampaign().has("drowned_captain"),"Both optional bosses beatable on "+klass+"/"+difficulty);
        }
        expansion("Mage","STANDARD",true,false,true);expansion("Rogue","STORY",true,true,false);
        System.out.println("PASS / "+assertions+" world checks: 8 expansion campaigns, six side quests, mutually exclusive branches, both optional bosses, peaceful paths, reputation, discount, rollback, migration, codex, sprites and audio.");
    }
}
