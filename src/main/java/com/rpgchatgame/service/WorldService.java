package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** World expansion choices are checked against the same data used by NPCs and the map. */
public final class WorldService {
    public static void prepare(GameState state){
        var w=state.getCampaign();
        if(!w.has("world14_initialized")){
            w.getReputation().putIfAbsent("camp",(w.has("survivors")?2:0)+(w.has("locket_returned")?1:0));
            w.getReputation().putIfAbsent("keepers",(w.has("runes")?1:0)+(w.has("archive")?1:0));w.getFlags().add("world14_initialized");
        }
        for(String name:w.getVisited())w.getCodex().add("location:"+WorldContent.location(name).id());
        w.getCodex().add("location:"+WorldContent.location(state.getCurrentLocation()).id());
        for(String id:WorldContent.MONSTERS.keySet())if(w.has(id))w.getCodex().add("enemy:"+id);
        if(w.has("rime_resolved"))w.getCodex().add("enemy:rime_sentinel");
        if(w.has("captain_resolved"))w.getCodex().add("enemy:drowned_captain");
        if(w.has("survivors"))w.getCodex().add("npc:mira");
    }
    public static int reputation(GameState state,String faction){return state.getCampaign().getReputation().getOrDefault(faction,0);}
    public static void reputation(GameState state,Map<String,Integer> changes){changes.forEach((id,delta)->state.getCampaign().getReputation().put(id,Math.max(0,Math.min(10,reputation(state,id)+delta))));}
    public static int price(GameState state,ItemCatalog.Entry entry){return reputation(state,"camp")>=3?Math.max(1,entry.price()*80/100):entry.price();}
    public static boolean available(GameState state,WorldContent.Action action){
        var w=state.getCampaign();return w.getEnemy()==null&&!w.epilogueClosed()&&WorldContent.location(state.getCurrentLocation()).id().equals(action.location())
                &&w.getFlags().containsAll(action.requires())&&action.absent().stream().noneMatch(w::has)
                &&action.items().stream().allMatch(id->ItemCatalog.owns(state,id))&&reputation(state,"camp")>=action.campMin()&&reputation(state,"keepers")>=action.keeperMin();
    }
    public static List<WorldContent.Npc> npcs(GameState state){return WorldContent.location(state.getCurrentLocation()).npcs().stream().map(WorldContent.NPCS::get).filter(n->state.getCampaign().getFlags().containsAll(n.requires())).toList();}
    public List<Choice> choices(GameState state){
        var w=state.getCampaign();List<Choice> result=new ArrayList<>();
        if(w.getEnemy()!=null||w.epilogueClosed())return result;
        if(!w.getDialogueNode().isBlank()){
            var node=WorldContent.DIALOGUES.get(w.getDialogueNode());
            for(String id:node.actions()){var action=WorldContent.ACTIONS.get(id);if(available(state,action))result.add(new Choice(action.label(),id));}
            result.add(new Choice("Leave conversation","dialogue_close"));return result;
        }
        for(var npc:npcs(state))result.add(new Choice("Talk to "+npc.name(),"npc_"+npc.id()));
        for(var action:WorldContent.ACTIONS.values())if(action.node().isBlank()&&available(state,action))result.add(new Choice(action.label(),action.id()));
        return result;
    }
    public static List<Choice> routes(GameState state){
        if(state.getCampaign().getEnemy()!=null||state.getCampaign().epilogueClosed()||!state.getCampaign().getDialogueNode().isBlank())return List.of();
        return WorldContent.location(state.getCurrentLocation()).routes().stream().filter(r->state.getCampaign().getFlags().containsAll(r.requires()))
                .map(r->new Choice("Travel to "+WorldContent.LOCATIONS.get(r.to()).name(),"go_"+r.to())).toList();
    }
    public boolean handle(GameState state,String id,StoryResponse response){
        var w=state.getCampaign();
        if(id.equals("explore_borderlands")){
            if(!w.epilogueClosed())return false;
            w.getFlags().add("borderlands_reopened");travel(state,"camp",response);response.setNarration("Your ending is preserved. New roads now lead beyond Emberwood; the mountain and coast await your help.");return true;
        }
        if(id.equals("dialogue_close")){if(w.getDialogueNode().isBlank())return false;w.setDialogueNode("");response.setNarration(WorldContent.location(state.getCurrentLocation()).description());return true;}
        if(id.startsWith("npc_")&&WorldContent.NPCS.containsKey(id.substring(4))){
            var npc=WorldContent.NPCS.get(id.substring(4));
            if(!w.getDialogueNode().isBlank()||w.getEnemy()!=null||w.epilogueClosed()||!npcs(state).contains(npc))return false;
            w.setDialogueNode(npc.dialogue());w.getCodex().add("npc:"+npc.id());
            response.setNarration(WorldContent.DIALOGUES.get(npc.dialogue()).text());return true;
        }
        if(id.startsWith("go_")&&WorldContent.LOCATIONS.containsKey(id.substring(3))){if(routes(state).stream().noneMatch(choice->choice.getAction().equals(id)))return false;travel(state,id.substring(3),response);return true;}
        var action=WorldContent.ACTIONS.get(id);if(action==null||choices(state).stream().noneMatch(choice->choice.getAction().equals(id)))return false;
        if(!action.enemy().isBlank()){
            var monster=WorldContent.MONSTERS.get(action.enemy());w.setDialogueNode("");new CombatService().begin(state,new Enemy(monster.id(),monster.name(),monster.hp(),monster.attack(),monster.defense()));
            w.getCodex().add("enemy:"+monster.id());response.setNarration(action.text());return true;
        }
        for(String item:action.consume())if(!new InventoryService().remove(state.getInventory(),item))throw new IllegalStateException("Required quest item missing");
        for(String flag:action.flags())w.mark(flag,"");if(!action.flags().isEmpty())w.getJournal().add(action.text());reputation(state,action.reputation());
        w.setDialogueNode(action.node());if(!action.npc().isBlank())w.getCodex().add("npc:"+action.npc());
        response.setNarration(action.text());response.setExpGain(action.exp());response.setGoldChange(action.gold());
        for(var item:action.rewards())response.getItemsGained().add(ItemCatalog.stack(item.id(),item.quantity()));
        return true;
    }
    public static void travel(GameState state,String locationId,StoryResponse response){
        var location=WorldContent.LOCATIONS.get(locationId);var w=state.getCampaign();w.setDialogueNode("");
        state.setCurrentLocation(location.name());state.getCharacter().setCurrentLocation(location.name());w.getVisited().add(location.name());w.getCodex().add("location:"+location.id());
        response.setNewLocation(location.name());response.setNarration(location.description());
    }
    public static WorldContent.Quest tracked(GameState state){
        var w=state.getCampaign();var pinned=WorldContent.QUESTS.get(w.getTrackedQuest());if(pinned!=null&&!w.has(pinned.id()))return pinned;
        return WorldContent.QUESTS.values().stream().filter(q->!w.has(q.id())).findFirst().orElse(null);
    }
}
