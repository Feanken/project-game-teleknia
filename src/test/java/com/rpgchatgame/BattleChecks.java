package com.rpgchatgame;

import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 1.3 combat/build/difficulty checks, fully headless and isolated from player saves. */
public final class BattleChecks {
    static int assertions;
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static DatabaseManager database()throws Exception{Path root=Path.of("target","battle-checks");Files.createDirectories(root);return new DatabaseManager(Files.createTempDirectory(root,"case-").resolve("save.db"));}
    static GameService game(String klass,String difficulty)throws Exception{var g=new GameService(database());g.startNewStory("Battle "+klass,klass,difficulty);return g;}
    static GameService fight(String klass,String difficulty)throws Exception{var g=game(klass,difficulty);g.performAction("go_gate");g.performAction("fight_wolf");g.getCurrentState().getCampaign().setEnemy(new Enemy("wolf","Test Wolf",200,14,2));return g;}
    static void act(GameService g,String action){g.performAction(action);}
    static void statusAndCooldown()throws Exception{
        var fighter=fight("Fighter","STANDARD");int hp=fighter.getCurrentState().getCharacter().getHp();act(fighter,"shield_bash");
        check(fighter.getCurrentState().getCharacter().getHp()==hp,"Bash stun skips current counterattack");
        check(fighter.getCurrentState().getCampaign().getTurn()==1,"Stun still spends a round");
        check(!fighter.getCurrentState().getCampaign().getEnemyEffects().containsKey("stun"),"One-round stun expires");
        check(CombatService.cooldown(fighter.getCurrentState(),"shield_bash")==3,"New skill cooldown requires three OTHER actions");
        int mp=fighter.getCurrentState().getCharacter().getMp();String world=JsonUtil.toJson(fighter.getCurrentState().getCampaign());
        act(fighter,"shield_bash");check(fighter.getCurrentState().getCharacter().getMp()==mp&&JsonUtil.toJson(fighter.getCurrentState().getCampaign()).equals(world),"Blocked cooldown spends nothing and does not tick effects");
        act(fighter,"guard");act(fighter,"guard");check(CombatService.cooldown(fighter.getCurrentState(),"shield_bash")==1,"Two other actions do not finish three-action cooldown");
        act(fighter,"guard");check(CombatService.cooldown(fighter.getCurrentState(),"shield_bash")==0,"Third other action completes cooldown");
        hp=fighter.getCurrentState().getCharacter().getHp();act(fighter,"shield_bash");check(fighter.getCurrentState().getCharacter().getHp()==hp,"Stun works after cooldown");
        var rogue=fight("Rogue","STANDARD");act(rogue,"venom_cut");
        var poison=rogue.getCurrentState().getCampaign().getEnemyEffects().get("poison");
        check(poison.getTurns()==2&&poison.getPotency()==5,"Poison application ticks once and leaves two rounds");
        check(rogue.getCurrentState().getCampaign().getEnemy().getHp()==190,"Venom direct and poison damage");
        act(rogue,"guard");act(rogue,"guard");check(!rogue.getCurrentState().getCampaign().getEnemyEffects().containsKey("poison"),"Poison expires after three valid rounds");
        check(rogue.getCurrentState().getCampaign().getEnemy().getHp()==180,"Poison has exactly three ticks");
        var mage=fight("Mage","STANDARD");act(mage,"skill");check(mage.getCurrentState().getCampaign().getEnemyEffects().get("burn").getTurns()==2,"Ember Surge applies Burn");
        var effects=new LinkedHashMap<String,StatusEffect>();CombatService.applyEffect(effects,"burn",3,4);CombatService.applyEffect(effects,"burn",2,7);CombatService.applyEffect(effects,"burn",3,2);
        check(effects.size()==1&&effects.get("burn").getPotency()==7&&effects.get("burn").getTurns()==3,"Effects refresh and retain highest potency without stacking");
        var ward=fight("Mage","STANDARD");ward.getCurrentState().getCampaign().getPlayerEffects().put("burn",new StatusEffect(2,4));act(ward,"mana_ward");
        check(ward.getCurrentState().getCharacter().getHp()==76,"Shield blocks direct damage but not Burn");
        check(ward.getCurrentState().getCampaign().getPlayerEffects().get("shield").getTurns()==1,"Shield lasts application and next round");
        act(ward,"guard");check(!ward.getCurrentState().getCampaign().getPlayerEffects().containsKey("shield"),"Shield expires even when enemy is charging");
        var shielded=fight("Rogue","STANDARD");shielded.getCurrentState().getCampaign().getEnemyEffects().put("shield",new StatusEffect(5,100));act(shielded,"venom_cut");
        check(shielded.getCurrentState().getCampaign().getEnemy().getHp()==195,"Poison bypasses enemy shield");
        act(shielded,"flee");check(shielded.getCurrentState().getCampaign().getPlayerEffects().isEmpty()&&shielded.getCurrentState().getCampaign().getEnemyEffects().isEmpty()&&shielded.getCurrentState().getCampaign().getCooldowns().isEmpty(),"Retreat clears all effects/cooldowns");
        var invalid=fight("Mage","STANDARD");invalid.getCurrentState().getCharacter().setMp(0);act(invalid,"frost_bind");
        check(invalid.getCurrentState().getCampaign().getTurn()==0&&invalid.getCurrentState().getCampaign().getEnemyEffects().isEmpty(),"Insufficient MP applies no stun/turn");
        act(invalid,"rally");check(invalid.getCurrentState().getCampaign().getTurn()==0,"Other-class action rejected");
    }
    static void buildAndGear()throws Exception{
        var g=game("Fighter","STANDARD");check(SkillCatalog.points(g.getCurrentState())==1,"Level 1 starts with one point");
        int attack=ItemCatalog.attack(g.getCurrentState());g.upgradeSkill("skill");check(SkillCatalog.points(g.getCurrentState())==0&&SkillCatalog.rank(g.getCurrentState(),"skill")==1,"Upgrade consumes point");
        check(ItemCatalog.attack(g.getCurrentState())==attack,"Skill rank does not change base attack");
        check(g.upgradeSkill("rally").contains("No skill points"),"No overspending points");
        check(g.upgradeSkill("venom_cut").contains("another class"),"No cross-class builds");
        act(g,"search");act(g,"rescue");act(g,"go_gate");act(g,"fight_wolf");
        while(g.getCurrentState().getCampaign().getEnemy()!=null)act(g,"skill");
        check(g.getCurrentState().getCharacter().getLevel()==2&&SkillCatalog.points(g.getCurrentState())==1,"Level up grants exactly one more point");
        check(g.resetBuild().contains("Camp"),"Reset requires camp");act(g,"go_camp");
        int gold=g.getCurrentState().getCharacter().getGold();g.getCurrentState().getCharacter().setHp(40);g.getCurrentState().getCharacter().setMp(10);
        g.resetBuild();check(g.getCurrentState().getCharacter().getGold()==gold-20&&SkillCatalog.points(g.getCurrentState())==2,"Paid reset refunds spent points");
        check(g.getCurrentState().getCharacter().getHp()==40&&g.getCurrentState().getCharacter().getMp()==10,"Reset cannot heal");
        check(g.resetBuild().contains("No allocated"),"Empty reset not charged");
        g.getCurrentState().getCharacter().setGold(200);g.buyItem("duelist_charm");g.buyItem("warding_charm");
        int beforeCharm=ItemCatalog.attack(g.getCurrentState());g.equipItem("duelist_charm");check(ItemCatalog.attack(g.getCurrentState())==beforeCharm+2&&ItemCatalog.criticalPeriod(g.getCurrentState())==3,"Duelist charm changes critical cadence");
        int a=ItemCatalog.attack(g.getCurrentState()),d=ItemCatalog.defense(g.getCurrentState());g.equipItem("warding_charm");
        check(ItemCatalog.attack(g.getCurrentState())==a-2&&ItemCatalog.defense(g.getCurrentState())==d+3,"Accessory replacement removes prior bonuses");
        g.buyItem("plate_armor");g.equipItem("plate_armor");check(ItemCatalog.defense(g.getCurrentState())==g.getCurrentState().getCharacter().getDefense()+11,"Armor and accessory bonuses combine");
        act(g,"go_trail");check(g.setDifficulty("STORY").contains("Camp"),"Difficulty only changes at camp");
        act(g,"go_camp");g.setDifficulty("STORY");check(CombatService.storyMode(g.getCurrentState()),"Difficulty switch persisted");
    }
    static void skillCoverageAndSafety()throws Exception{
        var rally=fight("Fighter","STANDARD");rally.getCurrentState().getCharacter().setHp(20);act(rally,"rally");
        check(rally.getCurrentState().getCharacter().getHp()==40&&rally.getCurrentState().getCharacter().getMp()==22,"Rally heal formula and MP cost");
        check(CombatService.cooldown(rally.getCurrentState(),"rally")==4,"Rally cooldown");
        var frost=fight("Mage","STANDARD");frost.getCurrentState().getCampaign().getEnemy().setDefense(99);act(frost,"frost_bind");
        check(frost.getCurrentState().getCampaign().getEnemy().getHp()==185,"Frost Bind ignores armor");
        check(frost.getCurrentState().getCharacter().getHp()==80&&CombatService.cooldown(frost.getCurrentState(),"frost_bind")==3,"Frost Bind stuns and cools down");
        var evasion=fight("Rogue","STANDARD");act(evasion,"evasion");
        check(evasion.getCurrentState().getCharacter().getHp()==88&&evasion.getCurrentState().getCampaign().getPlayerEffects().get("shield").getPotency()==10,"Evasion shield absorbs direct hit");
        var poison=game("Rogue","STANDARD");poison.getCurrentState().getCharacter().setLevel(3);poison.upgradeSkill("venom_cut");poison.upgradeSkill("venom_cut");poison.upgradeSkill("venom_cut");
        check(SkillCatalog.points(poison.getCurrentState())==0&&poison.upgradeSkill("venom_cut").contains("Maximum"),"Skill rank cap is enforced");
        act(poison,"go_gate");act(poison,"fight_wolf");poison.getCurrentState().getCampaign().setEnemy(new Enemy("wolf","Wolf",200,14,2));
        act(poison,"venom_cut");check(poison.getCurrentState().getCampaign().getEnemyEffects().get("poison").getPotency()==11,"Rank scaling changes poison potency");
        var dead=fight("Rogue","STANDARD");dead.getCurrentState().getCampaign().getEnemy().setHp(5);dead.getCurrentState().getCampaign().setTurn(1);
        dead.getCurrentState().getCharacter().setHp(1);dead.getCurrentState().getCampaign().getPlayerEffects().put("burn",new StatusEffect(1,4));dead.getCurrentState().getCampaign().getEnemyEffects().put("poison",new StatusEffect(1,5));act(dead,"guard");
        check(dead.getCurrentState().getCampaign().getEnemy()==null&&!dead.getCurrentState().getCampaign().has("wolf"),"Simultaneous damage-over-time death gives no victory");
        check(dead.getCurrentState().getCampaign().getPlayerEffects().isEmpty()&&dead.getCurrentState().getCampaign().getEnemyEffects().isEmpty(),"Defeat clears effects");
        var db=database();var reset=new GameService(db);reset.startNewStory();reset.upgradeSkill("skill");act(reset,"rescue");act(reset,"go_camp");String before=JsonUtil.toJson(reset.getCurrentState());
        try(var c=db.getConnection();var s=c.createStatement()){s.executeUpdate("CREATE TRIGGER fail_reset BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'failure'); END");}
        try{reset.resetBuild();throw new AssertionError("Reset save should fail");}catch(IllegalStateException expected){assertions++;}
        check(JsonUtil.toJson(reset.getCurrentState()).equals(before),"Failed reset preserves gold and ranks");
    }
    static void patternsAndDifficulty()throws Exception{
        var standard=fight("Fighter","STANDARD");var story=fight("Fighter","STORY");act(standard,"guard");act(story,"guard");
        check(story.getCurrentState().getCharacter().getHp()>standard.getCurrentState().getCharacter().getHp(),"Story reduces direct damage");
        check(CombatService.intent(standard.getCurrentState()).name().equals("Charge"),"Wolf advertises charge");
        int hp=standard.getCurrentState().getCharacter().getHp();act(standard,"guard");check(standard.getCurrentState().getCharacter().getHp()==hp,"Charge deals no direct damage");
        check(CombatService.intent(standard.getCurrentState()).name().equals("Heavy attack"),"Heavy attack follows charge");
        var warden=fight("Fighter","STANDARD");warden.getCurrentState().getCampaign().setEnemy(new Enemy("warden","Warden",200,20,2));
        warden.getCurrentState().getCampaign().getEnemy().setHp(101);act(warden,"attack");
        check(CombatService.intent(warden.getCurrentState()).name().equals("Crown fury"),"Boss enters Phase II below half HP");
        warden.getCurrentState().getCampaign().setTurn(2);act(warden,"guard");check(warden.getCurrentState().getCampaign().getPlayerEffects().containsKey("burn"),"Phase II heavy attack inflicts Burn");
        var guardian=fight("Fighter","STANDARD");guardian.getCurrentState().getCampaign().setEnemy(new Enemy("guardian","Guardian",200,14,2));guardian.getCurrentState().getCampaign().setTurn(3);
        check(CombatService.intent(guardian.getCurrentState()).defend(),"Guardian advertises defensive stance");act(guardian,"guard");
        check(guardian.getCurrentState().getCampaign().getEnemyEffects().get("shield").getTurns()==2,"Defensive stance grants shield for two future rounds");
        for(String difficulty:List.of("STANDARD","STORY")){
            var g=fight("Fighter",difficulty);g.getCurrentState().getCharacter().setHp(1);g.getCurrentState().getCharacter().setGold(100);act(g,"attack");
            check(g.getCurrentState().getCharacter().getGold()==(difficulty.equals("STORY")?90:80),"Difficulty defeat penalty");
            check(g.getCurrentState().getCampaign().getEnemy()==null,"Defeat resolves encounter");
            act(g,"go_gate");act(g,"fight_wolf");act(g,"flee");check(g.getCurrentState().getCharacter().getGold()==(difficulty.equals("STORY")?88:75),"Difficulty retreat penalty");
        }
        var early=fight("Fighter","STANDARD");early.getCurrentState().getCampaign().setTurn(12);early.getCurrentState().getCampaign().getEnemy().setAttack(20);act(early,"guard");
        var late=fight("Fighter","STANDARD");late.getCurrentState().getCampaign().setTurn(15);late.getCurrentState().getCampaign().getEnemy().setAttack(20);act(late,"guard");
        check(late.getCurrentState().getCharacter().getHp()<early.getCurrentState().getCharacter().getHp(),"Fatigue actually increases late-round damage");
        var exhausted=fight("Mage","STANDARD");exhausted.getCurrentState().getCampaign().getEnemy().setAttack(1);
        int rounds=0;while(exhausted.getCurrentState().getCampaign().getEnemy()!=null&&rounds++<400){
            var state=exhausted.getCurrentState();String action=state.getCharacter().getHp()<45&&state.getCharacter().getMp()>=8&&CombatService.cooldown(state,"heal")==0?"heal":"guard";act(exhausted,action);
        }
        check(rounds<400,"Guard/heal cannot create endless invulnerability");
    }
    static void persistenceAndMigration()throws Exception{
        var db=database();var g=new GameService(db);g.startNewStory("SaveMage","Mage","STORY");g.upgradeSkill("mana_ward");act(g,"go_gate");act(g,"fight_wolf");g.getCurrentState().getCampaign().setEnemy(new Enemy("wolf","Wolf",200,14,2));act(g,"mana_ward");g.saveSlot(2);
        var loaded=new GameService(db);loaded.loadSlot(2);
        check(JsonUtil.toJson(loaded.getCurrentState().getCampaign()).equals(JsonUtil.toJson(g.getCurrentState().getCampaign())),"Ranks, status, cooldown, difficulty and turn survive load");
        act(g,"guard");act(loaded,"guard");check(JsonUtil.toJson(g.getCurrentState().getCampaign()).equals(JsonUtil.toJson(loaded.getCurrentState().getCampaign()))&&g.getCurrentState().getCharacter().getHp()==loaded.getCurrentState().getCharacter().getHp(),"Replay is deterministic after load");
        String before=JsonUtil.toJson(g.getCurrentState());
        try(var c=db.getConnection();var s=c.createStatement()){s.executeUpdate("CREATE TRIGGER fail_battle BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'failure'); END");}
        try{act(g,"guard");throw new AssertionError("Save must fail");}catch(IllegalStateException expected){assertions++;}
        check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Failed combat save rolls back effects and cooldowns in memory");
        var old=game("Rogue","STANDARD");act(old,"go_gate");act(old,"fight_wolf");act(old,"attack");
        Map<String,Object> payload=JsonUtil.fromJson(JsonUtil.toJson(old.getCurrentState()),Map.class);
        Map<String,Object> world=(Map<String,Object>)payload.get("campaign");for(String field:List.of("difficulty","accessory","skillRanks","cooldowns","playerEffects","enemyEffects"))world.remove(field);
        String json=JsonUtil.toJson(payload);String checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));
        var target=database();target.initialize();
        try(var c=target.getConnection();var s=c.prepareStatement("INSERT INTO save_slots(slot,version,saved_at,payload,checksum) VALUES(0,2,'2026-10-29T00:00:00Z',?,?)")){s.setString(1,json);s.setString(2,checksum);s.executeUpdate();}
        var migrated=new GameService(target).continueStory().orElseThrow();
        check(migrated.getCampaign().getDifficulty().equals("STANDARD")&&migrated.getCampaign().getSkillRanks().isEmpty(),"1.2 schema migrates to Standard with unspent points");
        check(migrated.getCampaign().getEnemy().getHp()==old.getCurrentState().getCampaign().getEnemy().getHp()&&migrated.getCampaign().getTurn()==1,"Migration preserves mid-combat HP and turn");
    }
    static void battle(GameService g,String flag){int turns=0;while(g.getCurrentState().getCampaign().getEnemy()!=null&&turns++<80){var s=g.getCurrentState();String move="skill";
        if(s.getCharacter().getHp()<s.getCharacter().getMaxHp()/3&&ItemCatalog.owns(s,"small_potion"))move="potion";
        else if(s.getCharacter().getHp()<s.getCharacter().getMaxHp()/3&&s.getCharacter().getMp()>=8&&CombatService.cooldown(s,"heal")==0)move="heal";
        else if(CombatService.intent(s).bonus()>0||s.getCharacter().getMp()<6)move="guard";
        act(g,move);
    }check(turns<80&&g.getCurrentState().getCampaign().has(flag),"Encounter beatable "+g.getCurrentState().getCharacter().getCharacterClass()+"/"+g.getCurrentState().getCampaign().getDifficulty()+"/"+flag);}
    static void campaign(String klass,String difficulty)throws Exception{
        var g=game(klass,difficulty);g.upgradeSkill("skill");act(g,"search");act(g,"rescue");act(g,"go_gate");act(g,"fight_wolf");battle(g,"wolf");
        act(g,"go_camp");g.buyItem("iron_sword");g.buyItem("ranger_armor");g.equipItem("iron_sword");g.equipItem("ranger_armor");act(g,"rest");act(g,"talk_mira");
        act(g,"go_trail");act(g,"herbs");act(g,"go_shrine");act(g,"runes");act(g,"fight_guardian");battle(g,"guardian");g.equipItem("ember_blade");
        act(g,"sigil");act(g,"go_ruins");act(g,"archive");act(g,"locket");act(g,"go_camp");act(g,"return_locket");act(g,"rest");act(g,"go_ruins");act(g,"go_tower");act(g,"fight_warden");battle(g,"warden");act(g,"end_purify");
        check(g.getCurrentState().getCampaign().getEnding().equals("Dawnkeeper"),"Campaign complete "+klass+"/"+difficulty);
    }
    public static void main(String[] args)throws Exception{
        statusAndCooldown();buildAndGear();skillCoverageAndSafety();patternsAndDifficulty();persistenceAndMigration();
        for(String klass:List.of("Fighter","Rogue","Mage"))for(String difficulty:List.of("STANDARD","STORY"))campaign(klass,difficulty);
        System.out.println("PASS / "+assertions+" battle checks: skill ranks, status lifecycles, cooldowns, patterns, accessories, difficulty, replay, migration and 6 complete campaigns.");
    }
}
