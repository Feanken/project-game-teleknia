package com.rpgchatgame.service;

import com.rpgchatgame.util.JsonUtil;
import java.util.*;
import java.util.function.Function;

/** Validated graph, characters, quest milestones, encounters and dialogue/action data. */
public final class WorldContent {
    public record Route(String to,List<String> requires) {}
    public record Location(String id,String name,String region,String sprite,String description,List<String> npcs,List<Route> routes) {}
    public record Npc(String id,String name,String location,String sprite,List<String> requires,String dialogue,String lore) {}
    public record Quest(String id,String title,String description,String npc,String region) {}
    public record Reward(String id,int quantity) {}
    public record Monster(String id,String name,int hp,int attack,int defense,int exp,int gold,String sprite,String pattern,List<String> flags,Map<String,Integer> reputation,List<Reward> items,String lore) {}
    public record Action(String id,String label,String location,String text,List<String> requires,List<String> absent,List<String> items,List<String> consume,List<String> flags,int exp,int gold,Map<String,Integer> reputation,List<Reward> rewards,String node,String npc,int keeperMin,int campMin,String enemy) {}
    public record Dialogue(String id,String npc,String text,List<String> actions) {}
    public record Event(String text,int exp,int gold,Map<String,Integer> reputation) {}
    public static final Map<String,Location> LOCATIONS=index(read("locations",Location[].class),Location::id);
    public static final Map<String,Npc> NPCS=index(read("npcs",Npc[].class),Npc::id);
    public static final Map<String,Quest> QUESTS=index(read("quests",Quest[].class),Quest::id);
    public static final Map<String,Monster> MONSTERS=index(read("enemies",Monster[].class),Monster::id);
    public static final Map<String,Action> ACTIONS=index(read("world-actions",Action[].class),Action::id);
    public static final Map<String,Dialogue> DIALOGUES=index(read("dialogues",Dialogue[].class),Dialogue::id);
    public static final Map<String,Event> EVENTS=events();
    static{validate();}
    private WorldContent(){}
    public static <T>T read(String name,Class<T> type){try(var input=WorldContent.class.getResourceAsStream("/definitions/"+name+".json")){
        if(input==null)throw new IllegalStateException("Missing world data: "+name);
        return JsonUtil.fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),type);
    }catch(java.io.IOException error){throw new ExceptionInInitializerError(error);}}
    private static <T>Map<String,T> index(T[] rows,Function<T,String> id){
        Map<String,T> result=new LinkedHashMap<>();for(T row:rows){String key=id.apply(row);if(key==null||key.isBlank()||result.putIfAbsent(key,row)!=null)throw new IllegalStateException("Duplicate/blank content ID");}return Collections.unmodifiableMap(result);
    }
    @SuppressWarnings("unchecked") private static Map<String,Event> events(){
        Map<String,Object> rows=read("events",Map.class);Map<String,Event> result=new LinkedHashMap<>();
        rows.forEach((key,value)->result.put(key,JsonUtil.fromJson(JsonUtil.toJson(value),Event.class)));return Collections.unmodifiableMap(result);
    }
    public static Location location(String name){return LOCATIONS.values().stream().filter(l->l.name().equals(name)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown location: "+name));}
    public static String travelId(String name){return "go_"+location(name).id();}
    private static void require(boolean valid,String problem){if(!valid)throw new IllegalStateException("World data: "+problem);}
    private static void rewards(List<Reward> rewards){for(var reward:rewards)require(ItemCatalog.entry(reward.id())!=null&&reward.quantity()>0,"Invalid reward item");}
    public static void validate(){
        Set<String> flags=new HashSet<>(EVENTS.keySet());flags.addAll(QUESTS.keySet());flags.addAll(MONSTERS.keySet());
        ACTIONS.values().forEach(a->flags.addAll(a.flags()));MONSTERS.values().forEach(m->flags.addAll(m.flags()));
        for(var location:LOCATIONS.values()){
            require(location.name()!=null&&location.region()!=null&&location.sprite()!=null&&!location.description().isBlank(),"Incomplete location");
            for(var route:location.routes())require(LOCATIONS.containsKey(route.to())&&flags.containsAll(route.requires()),"Invalid route or gate");
            for(String npc:location.npcs())require(NPCS.containsKey(npc)&&NPCS.get(npc).location().equals(location.id()),"NPC placement mismatch");
        }
        Set<String> reached=new HashSet<>();Deque<String> queue=new ArrayDeque<>();queue.add("village");
        while(!queue.isEmpty()){String id=queue.remove();if(reached.add(id))LOCATIONS.get(id).routes().forEach(r->queue.add(r.to()));}
        require(reached.size()==LOCATIONS.size(),"Unreachable location");
        for(var npc:NPCS.values())require(LOCATIONS.containsKey(npc.location())&&DIALOGUES.containsKey(npc.dialogue())&&flags.containsAll(npc.requires()),"Invalid NPC reference");
        for(var quest:QUESTS.values())require(quest.npc().isBlank()||NPCS.containsKey(quest.npc()),"Quest NPC missing");
        for(var monster:MONSTERS.values()){require(Set.of("wolf","guardian","warden","captain","scorcher","venom","sentinel","breaker","siphon","duelist").contains(monster.pattern()),"Invalid enemy pattern");require(monster.hp()>0&&monster.attack()>0&&monster.defense()>=0&&monster.exp()>=0&&monster.gold()>=0,"Invalid encounter stats");rewards(monster.items());}
        for(var action:ACTIONS.values()){
            require(LOCATIONS.containsKey(action.location())&&flags.containsAll(action.requires())&&flags.containsAll(action.absent()),"Invalid action gates");
            require(action.exp()>=0&&action.gold()>=0&&action.campMin()>=0&&action.campMin()<=10&&action.keeperMin()>=0&&action.keeperMin()<=10,"Invalid reward/reputation gate");
            require(action.node().isBlank()||DIALOGUES.containsKey(action.node()),"Missing next dialogue");
            require(action.npc().isBlank()||NPCS.containsKey(action.npc()),"Missing speaker");
            require(action.enemy().isBlank()||MONSTERS.containsKey(action.enemy()),"Missing encounter");
            for(String item:action.items())require(ItemCatalog.entry(item)!=null,"Missing required item");
            for(String item:action.consume())require(action.items().contains(item),"Consumed item not required");rewards(action.rewards());
            if(action.exp()>0||action.gold()>0||!action.rewards().isEmpty()||!action.reputation().isEmpty())require(action.absent().stream().anyMatch(action.flags()::contains),"Reward action missing a one-time gate");
            action.reputation().forEach((faction,delta)->require(Set.of("camp","keepers").contains(faction)&&Math.abs(delta)<=10,"Unknown faction"));
        }
        for(var node:DIALOGUES.values()){
            require(NPCS.containsKey(node.npc()),"Dialogue NPC missing");
            for(String action:node.actions())require(ACTIONS.containsKey(action)&&ACTIONS.get(action).location().equals(NPCS.get(node.npc()).location()),"Dialogue/action location mismatch");
        }
    }
}
