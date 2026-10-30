package com.rpgchatgame;
import com.rpgchatgame.service.*;
import com.rpgchatgame.model.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;

public final class LivingChecks {
    static int count;
    static void check(boolean ok,String message){count++;if(!ok)throw new AssertionError(message);}
    static GameService game()throws Exception{var g=new GameService(WorldChecks.database());g.startNewStory();return g;}
    static void location(GameService g,String id){var s=g.getCurrentState();s.setCurrentLocation(WorldContent.LOCATIONS.get(id).name());s.getCampaign().setDialogueNode("");}
    static void act(GameService g,String id){g.performAction(id);}
    public static void main(String[] args)throws Exception{
        check(LivingService.DIALOGUES.size()>=20&&LivingService.EVENTS.size()==6,"Context and event targets");
        for(var d:LivingService.DIALOGUES){
            var g=game();var n=WorldContent.NPCS.get(d.npc());location(g,n.location());var w=g.getCurrentState().getCampaign();
            w.getFlags().addAll(n.requires());w.getFlags().addAll(d.requires());w.setDialogueNode(n.dialogue());w.getReputation().put("camp",5);
            if(d.ending()){w.setEnding("LEGACY ENDING");w.mark("borderlands_reopened","");}
            act(g,"look");check(g.getCurrentState().getCurrentChoices().stream().anyMatch(c->c.getAction().equals("context_"+d.id())),"Context offered: "+d.id());
            int gold=g.getCurrentState().getCharacter().getGold(),xp=g.getCurrentState().getCharacter().getExp();act(g,"context_"+d.id());
            check(g.getCurrentState().getCurrentStorySummary().contains(d.text()),"Full contextual text recorded");act(g,"context_"+d.id());
            check(g.getCurrentState().getCharacter().getGold()==gold&&g.getCurrentState().getCharacter().getExp()==xp,"Reading cannot farm rewards");
            g.saveSlot(1);g.loadSlot(1);check(g.getCurrentState().getCampaign().getDialogueNode().equals(n.dialogue()),"Reading preserves NPC/save context");
        }
        for(var e:LivingService.EVENTS)for(String branch:List.of("help","note")){
            var g=game();location(g,e.location());act(g,"event_"+branch+"_"+e.id());check(!g.getCurrentState().getCampaign().has("event16_"+e.id()),"Locked event rejected");
            g.getCurrentState().getCampaign().mark(e.gate(),"");act(g,"look");int gold=g.getCurrentState().getCharacter().getGold();act(g,"event_"+branch+"_"+e.id());
            check(g.getCurrentState().getCharacter().getGold()==gold+(branch.equals("help")?4:0),"Branch reward exact");g.saveSlot(2);g.loadSlot(2);act(g,"event_help_"+e.id());
            check(g.getCurrentState().getCharacter().getGold()==gold+(branch.equals("help")?4:0),"Alternate branch/reload cannot duplicate event reward");
        }
        for(var c:BondService.COMPANIONS)for(String ending:List.of("home","watch")){
            var g=game();location(g,"camp");var w=g.getCurrentState().getCampaign();w.getFlags().addAll(c.bonds());w.mark(c.gate(),"");
            g.companion("recruit",c.id());g.companion("activate",c.id());g.companion("bond",c.id());check(!g.getCurrentState().getCampaign().has("bond_"+c.id()),"Field stage required before new bond");
            g.companion("talk",c.id());g.saveSlot(1);g.loadSlot(1);String objective=WorldContent.location(LivingService.objective(c.id())).id();location(g,objective);act(g,"journey_"+c.id());
            check(g.getCurrentState().getCampaign().has("journey_field_"+c.id()),"Field objective recorded with active companion");g.saveSlot(1);g.loadSlot(1);location(g,"camp");act(g,"promise_"+ending+"_"+c.id());
            check(g.getCurrentState().getCampaign().has("journey_end_"+c.id())&&g.getCurrentState().getCampaign().has("bond_"+c.id()),"Personal journey and bonus complete");
            act(g,"promise_"+(ending.equals("home")?"watch":"home")+"_"+c.id());check(!g.getCurrentState().getCampaign().has("promise_"+(ending.equals("home")?"watch":"home")+"_"+c.id()),"Promise outcomes exclusive");
        }
        var db=WorldChecks.database();var g=new GameService(db);g.startNewStory();location(g,"village");g.getCurrentState().getCampaign().mark("survivors","");act(g,"look");String before=JsonUtil.toJson(g.getCurrentState());
        try(var c=db.getConnection();var q=c.createStatement()){q.executeUpdate("CREATE TRIGGER fail_event BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'event rollback'); END");}
        try{act(g,"event_help_embers");throw new AssertionError("Save should fail");}catch(SaveGameRepository.SaveProblem expected){count++;}
        check(JsonUtil.toJson(g.getCurrentState()).equals(before),"Failed event save preserves reward/flags/logs");
        var legacy=game();location(legacy,"camp");legacy.getCurrentState().getCampaign().getFlags().addAll(List.of("survivors","recruit_mira","bond_mira"));legacy.saveSlot(1);legacy.loadSlot(1);
        check(legacy.getCurrentState().getCampaign().has("bond_mira")&&!legacy.getCurrentState().getCampaign().has("journey_field_mira"),"Legacy support bonus preserved without inventing field progress");
        System.out.println("PASS / "+count+" living checks: contextual dialogues, event branches, staged companion promises, legacy bonus, save/load and rollback.");
    }
}
