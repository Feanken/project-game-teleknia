package com.rpgchatgame;

import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.util.JsonUtil;
import com.rpgchatgame.world.*;
import java.util.*;
import static com.rpgchatgame.world.WorldRules.*;

public final class FoundationChecks {
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static void rejects(Runnable code,String text){try{code.run();throw new AssertionError(text);}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        var unopened=new GameService(WorldChecks.database());rejects(()->unopened.performAction(""),"Invalid command before load rejected");check(!unopened.hasCurrentGame()&&!unopened.hasSavedGame(),"Invalid command cannot implicitly create a campaign");
        var game=new GameService(WorldChecks.database());game.startNewStory();var source=game.snapshot();String original=JsonUtil.toJson(source);long revision=game.saveRevision();
        var next=new CampaignEngine().advance(source,"search");
        check(JsonUtil.toJson(source).equals(original),"Rules leave source snapshot untouched");
        check(JsonUtil.toJson(game.snapshot()).equals(original)&&game.saveRevision()==revision,"Rules cannot publish/save candidate");
        check(next.state().getCampaign().has("supplies")&&!source.getCampaign().has("supplies"),"Rules produce a detached progressed candidate");
        next.state().getCharacter().setGold(0);check(game.snapshot().getCharacter().getGold()==20,"Discarded candidate cannot change live game");
        source.getInventory().clear();source.getCampaign().getFlags().add("test");check(!game.snapshot().getInventory().isEmpty()&&!game.snapshot().getCampaign().has("test"),"Read model deeply detached");
        game.performAction("search");check(game.snapshot().getCampaign().has("supplies")&&game.saveRevision()>revision,"Existing facade commits candidate");
        var s=game.snapshot();check(GameplayMode.of(s)==GameplayMode.EXPLORATION,"Exploration mode");s.getCampaign().setDialogueNode("mira");check(GameplayMode.of(s)==GameplayMode.DIALOGUE,"Dialogue mode");s.getCampaign().setDialogueNode("");s.getCampaign().setEnding("Dawnkeeper");check(GameplayMode.of(s)==GameplayMode.EPILOGUE,"Closed epilogue mode");s.getCampaign().getFlags().add("borderlands_reopened");check(GameplayMode.of(s)==GameplayMode.EXPLORATION,"Reopened ending explores");s.getCampaign().setEnemy(new Enemy("wolf","Wolf",38,14,2));check(GameplayMode.of(s)==GameplayMode.BATTLE,"Encounter mode has priority");
        rejects(()->new CampaignEngine().advance(game.snapshot(),""),"Empty commands rejected");
        var tiles=new ArrayList<>(List.of(1,1,1,1,1,1));var empty=Collections.nCopies(6,0);var blocked=new ArrayList<>(Collections.nCopies(6,false));blocked.set(5,true);
        var spawn=List.of(new MapDefinition.Spawn("entry",new Tile(0,0),Direction.DOWN));
        var map=new MapDefinition(1,"village",3,2,tiles,empty,empty,blocked,spawn,List.of(new MapDefinition.Portal("exit",new Tile(2,0),"gate","entry")),List.of(new MapDefinition.Interaction("mira",MapDefinition.Kind.NPC,new Tile(1,1),"npc_mira","mira")));
        check(map.walkable(new Tile(0,0))&&!map.walkable(new Tile(2,1))&&!map.walkable(new Tile(-1,0)),"Collision contract includes map boundary");
        tiles.set(0,99);blocked.set(0,true);check(map.ground().getFirst()==1&&map.walkable(new Tile(0,0)),"Map defensively copies authored data");
        check(JsonUtil.fromJson(JsonUtil.toJson(map),MapDefinition.class).equals(map),"Map JSON round trip");
        check(new Tile(1,1).facing(Direction.UP).equals(new Tile(1,0))&&new Tile(1,1).facing(Direction.LEFT).equals(new Tile(0,1)),"Facing uses tile coordinates");
        rejects(()->new MapDefinition(2,"village",3,2,map.ground(),empty,empty,map.blocked(),spawn,List.of(),List.of()),"Future format rejected");
        rejects(()->new MapDefinition(1,"village",3,2,List.of(1),empty,empty,map.blocked(),spawn,List.of(),List.of()),"Incomplete layer rejected");
        rejects(()->new MapDefinition(1,"village",3,2,map.ground(),empty,empty,map.blocked(),List.of(new MapDefinition.Spawn("entry",new Tile(2,1),Direction.UP)),List.of(),List.of()),"Blocked spawn rejected");
        rejects(()->new MapDefinition(1,"village",3,2,map.ground(),empty,empty,map.blocked(),List.of(spawn.getFirst(),spawn.getFirst()),List.of(),List.of()),"Duplicate spawn rejected");
        rejects(()->new MapDefinition(1,"village",3,2,map.ground(),empty,empty,map.blocked(),spawn,List.of(new MapDefinition.Portal("exit",new Tile(4,0),"gate","entry")),List.of()),"Out of bounds portal rejected");
        System.out.println("PASS / "+checks+" foundation checks: detached rules/state, publish boundary, modes and validated map contracts.");
    }
}
