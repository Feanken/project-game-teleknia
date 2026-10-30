package com.rpgchatgame;
import com.rpgchatgame.service.*;
import com.rpgchatgame.model.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class MasteryChecks {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void act(GameService g,String id){g.performAction(id);}
    static GameService fixture(String klass)throws Exception{
        var g=new GameService(WorldChecks.database());g.startNewStory("Mastery",klass);var s=g.getCurrentState();
        new CharacterService().applyProgress(s.getCharacter(),0,0,400,500);s.setCurrentLocation("Survivor Camp");s.getCampaign().getFlags().add("guardian");act(g,"look");return g;
    }
    static void battle(GameService g,String flag){int n=0;while(g.getCurrentState().getCampaign().getEnemy()!=null&&n++<100){
        var s=g.getCurrentState();var h=s.getCharacter();String move="skill";
        if(h.getHp()<h.getMaxHp()/3&&h.getMp()>=8&&CombatService.cooldown(s,"heal")==0)move="heal";
        else if(h.getHp()<h.getMaxHp()/3&&ItemCatalog.owns(s,"small_potion"))move="potion";
        else if(CombatService.intent(s).bonus()>0||h.getMp()<6)move="guard";
        else if(!s.getCampaign().getCompanion().isBlank()&&CombatService.unavailable(s,"companion").isBlank())move="companion";
        else if(MasteryService.active(s)!=null&&CombatService.unavailable(s,"mastery").isBlank())move="mastery";
        act(g,move);
    }check(n<100&&g.getCurrentState().getCampaign().has(flag),"Fight cleared: "+flag);}
    static void gates()throws Exception{
        var db=WorldChecks.database();var g=new GameService(db);g.startNewStory();
        check(g.chooseSpecialization("guardian").contains("level 5"),"Level/milestone gate");
        var s=g.getCurrentState();new CharacterService().applyProgress(s.getCharacter(),0,0,400,100);s.getCampaign().getFlags().add("guardian");
        check(g.chooseSpecialization("guardian").contains("Survivor Camp"),"Camp gate");s.setCurrentLocation("Survivor Camp");s.getCharacter().setHp(31);s.getCharacter().setMp(7);
        int gold=s.getCharacter().getGold();g.chooseSpecialization("guardian");s=g.getCurrentState();
        check(MasteryService.is(s,"guardian")&&s.getCharacter().getGold()==gold,"First choice free");
        check(s.getCharacter().getHp()==31&&s.getCharacter().getMp()==7,"No resource refill");
        check(g.chooseSpecialization("pyromancer").contains("your class"),"Cross-class gate");
        g.chooseSpecialization("berserker");check(g.getCurrentState().getCharacter().getGold()==gold-30,"Switch costs exactly 30");
        long revision=g.saveRevision();g.chooseSpecialization("berserker");check(g.saveRevision()==revision,"Same path read-only");
        g.saveSlot(1);g.loadSlot(1);check(MasteryService.is(g.getCurrentState(),"berserker"),"Manual slot preserves path");
        String before=JsonUtil.toJson(g.getCurrentState());
        try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("CREATE TRIGGER mastery_failure BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'test'); END");}
        try{g.chooseSpecialization("guardian");throw new AssertionError("Expected save failure");}catch(SaveGameRepository.SaveProblem expected){checks++;}
        check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Failed save rolls back path and fee");
        try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("DROP TRIGGER mastery_failure");}
        g.getCurrentState().getCharacter().setGold(29);check(g.chooseSpecialization("guardian").contains("30 gold"),"Insufficient fee preserves build");
        g.startNewStory();check(g.getCurrentState().getCampaign().getSpecialization().isBlank(),"New journey resets mastery");
        @SuppressWarnings("unchecked") Map<String,Object> payload=JsonUtil.fromJson(JsonUtil.toJson(g.getCurrentState()),Map.class);
        ((Map<?,?>)payload.get("campaign")).remove("specialization");String json=JsonUtil.toJson(payload);
        String checksum=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));
        try(var c=db.getConnection();var q=c.prepareStatement("UPDATE save_slots SET version=5,payload=?,checksum=? WHERE slot=0")){q.setString(1,json);q.setString(2,checksum);q.executeUpdate();}
        g.loadSlot(0);check(g.getCurrentState().getCampaign().getSpecialization().isBlank(),"Schema 5 migration starts without forced path");
    }
    static void combat()throws Exception{
        for(var p:MasteryService.PATHS){var g=fixture(p.klass());g.chooseSpecialization(p.id());var s=g.getCurrentState();
            new CombatService().begin(s,new Enemy("wolf","Mastery dummy",1000,14,2));act(g,"look");
            check(g.getCurrentState().getCurrentChoices().stream().anyMatch(c->c.getAction().equals("mastery")),"Active choice: "+p.id());
            int mp=s.getCharacter().getMp();act(g,"mastery");s=g.getCurrentState();
            check(s.getCharacter().getMp()==mp-p.mp()&&CombatService.cooldown(s,"mastery")==3,"Exact MP/cooldown: "+p.id());
            String world=JsonUtil.toJson(s.getCampaign());act(g,"mastery");check(JsonUtil.toJson(g.getCurrentState().getCampaign()).equals(world),"Cooldown blocks effect and turn: "+p.id());
            g.saveSlot(2);g.loadSlot(2);check(CombatService.cooldown(g.getCurrentState(),"mastery")==3,"Combat cooldown survives load");
            check(!MasteryService.blocked(g.getCurrentState(),MasteryService.PATHS.stream().filter(x->x.klass().equals(p.klass())&&!x.id().equals(p.id())).findFirst().orElseThrow().id()).isBlank(),"No midbattle switching");
            act(g,"guard");act(g,"guard");act(g,"guard");check(CombatService.cooldown(g.getCurrentState(),"mastery")==0,"Three other actions restore skill");
            act(g,"flee");check(g.getCurrentState().getCampaign().getCooldowns().isEmpty()&&MasteryService.is(g.getCurrentState(),p.id()),"Retreat clears effects, keeps path");
            s=g.getCurrentState();new CombatService().begin(s,new Enemy("wolf","Dummy",1000,14,2));s.getCharacter().setMp(0);act(g,"mastery");check(g.getCurrentState().getCampaign().getTurn()==0,"No MP cannot spend turn");
        }
        for(String pattern:List.of("scorcher","venom","sentinel","breaker","siphon","duelist")){
            var m=WorldContent.MONSTERS.values().stream().filter(x->x.pattern().equals(pattern)).findFirst().orElseThrow();var g=fixture("Fighter");var s=g.getCurrentState();new CombatService().begin(s,new Enemy(m.id(),m.name(),m.hp(),m.attack(),m.defense()));
            int mp=s.getCharacter().getMp();act(g,"guard");s=g.getCurrentState();
            if(pattern.equals("scorcher")||pattern.equals("venom"))check(!s.getCampaign().getPlayerEffects().isEmpty(),"Encounter applies status: "+pattern);
            if(pattern.equals("sentinel"))check(s.getCampaign().getEnemyEffects().containsKey("shield"),"Mirror ward works");
            if(pattern.equals("siphon"))check(s.getCharacter().getMp()<mp,"Mana leech works");
            if(pattern.equals("breaker")){s.getCampaign().setTurn(2);check(CombatService.intent(s).name().equals("Armor break"),"Armor break telegraphed");}
            if(pattern.equals("duelist"))check(CombatService.intent(s).bonus()>0,"Riposte telegraphed");
        }
    }
    static void campaignMatrix()throws Exception{
        for(var p:MasteryService.PATHS)for(String difficulty:List.of("STANDARD","STORY"))for(boolean ally:List.of(false,true)){
            var g=WorldChecks.expansion(p.klass(),difficulty,false,true,true);check(g.getCurrentState().getCharacter().getLevel()>=5,"Level 5 earned through gameplay");
            check(g.chooseSpecialization(p.id()).startsWith("Specialization:"),"Path unlocked naturally");
            if(ally){g.companion("recruit","sera");g.companion("activate","sera");check(g.getCurrentState().getCampaign().getCompanion().equals("sera"),"Companion fixture active");}
            act(g,"rest");act(g,"go_ruins");act(g,"archive");act(g,"go_tower");act(g,"fight_warden");battle(g,"warden");act(g,"end_seal");check(!g.getCurrentState().getCampaign().getEnding().isBlank(),p.id()+" / "+difficulty+" / companion="+ally);
        }
    }
    static void passivesAndHunts()throws Exception{
        for(var p:MasteryService.PATHS){var g=fixture(p.klass());var base=g.getCurrentState();var active=JsonUtil.fromJson(JsonUtil.toJson(base),GameState.class);active.getCampaign().setSpecialization(p.id());
            for(var s:List.of(base,active)){new CombatService().begin(s,new Enemy("wolf","Dummy",1000,20,2));s.getCampaign().setTurn(2);s.getCharacter().setHp(s.getCharacter().getMaxHp()/2);}
            String move=p.id().equals("arcanist")?"spell":p.id().equals("pyromancer")?"skill":"attack";
            var normal=new StoryResponse();var boosted=new StoryResponse();new CombatService().resolve(base,move,normal);new CombatService().resolve(active,move,boosted);
            switch(p.id()){
                case "guardian"->check(boosted.getHpChange()==normal.getHpChange()+3,"Guardian passive mitigation");
                case "arcanist"->check(boosted.getMpChange()==normal.getMpChange()+2,"Arcanist passive discount");
                case "pyromancer"->check(active.getCampaign().getEnemyEffects().get("burn").getPotency()==6,"Pyromancer passive burn");
                default->check(active.getCampaign().getEnemy().getHp()==base.getCampaign().getEnemy().getHp()-(p.id().equals("ranger")?3:p.id().equals("assassin")?6:4),"Passive damage: "+p.id());
            }
        }
        for(var a:WorldContent.ACTIONS.values())if(a.id().startsWith("hunt_")){
            var g=fixture("Mage");g.chooseSpecialization("pyromancer");var s=g.getCurrentState();s.setCurrentLocation(WorldContent.LOCATIONS.get(a.location()).name());act(g,"look");
            check(s.getCampaign().getEnemy()==null,"Hunts require explicit action");act(g,a.id());check(g.getCurrentState().getCampaign().getEnemy()!=null,"Hunt begins at correct location");battle(g,a.enemy());
            int gold=g.getCurrentState().getCharacter().getGold();act(g,a.id());check(g.getCurrentState().getCampaign().getEnemy()==null&&g.getCurrentState().getCharacter().getGold()==gold,"Hunt reward cannot repeat");
        }
    }
    public static void main(String[] args)throws Exception{gates();combat();passivesAndHunts();campaignMatrix();System.out.println("PASS / "+checks+" mastery checks: migration, atomic fees, cooldowns, patterns, six paths x two difficulties x solo/companion.");}
}
