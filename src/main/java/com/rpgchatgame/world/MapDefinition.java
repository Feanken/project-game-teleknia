package com.rpgchatgame.world;

import java.util.*;
import static com.rpgchatgame.world.WorldRules.*;

/** Immutable JSON-ready contract; concrete playable maps arrive in stage 2. Coordinates are tiles. */
public record MapDefinition(int format,String id,int width,int height,
        List<Integer> ground,List<Integer> objects,List<Integer> foreground,List<Boolean> blocked,
        List<Spawn> spawns,List<Portal> portals,List<Interaction> interactions) {
    public record Spawn(String id,Tile tile,Direction facing){}
    public record Portal(String id,Tile tile,String targetMap,String targetSpawn){}
    public enum Kind { NPC, CHEST, INSPECT, ENCOUNTER }
    public record Interaction(String id,Kind kind,Tile tile,String action,String sprite){}
    public MapDefinition {
        if(format!=MAP_FORMAT||!identifier(id)||width<1||height<1||width>256||height>256||width*height>16384)throw new IllegalArgumentException("Invalid map header");
        ground=List.copyOf(ground);objects=List.copyOf(objects);foreground=List.copyOf(foreground);blocked=List.copyOf(blocked);
        spawns=List.copyOf(spawns);portals=List.copyOf(portals);interactions=List.copyOf(interactions);
        for(var layer:List.of(ground,objects,foreground))if(layer.size()!=width*height||layer.stream().anyMatch(v->v<0))throw new IllegalArgumentException("Invalid tile layer");
        if(blocked.size()!=width*height||spawns.isEmpty())throw new IllegalArgumentException("Missing collision/spawn data");
        Set<String> keys=new HashSet<>();
        for(var spawn:spawns){unique(keys,spawn.id());walkable(spawn.tile(),width,height,blocked);Objects.requireNonNull(spawn.facing());}
        keys.clear();for(var portal:portals){unique(keys,portal.id());walkable(portal.tile(),width,height,blocked);if(!identifier(portal.targetMap())||!identifier(portal.targetSpawn()))throw new IllegalArgumentException("Invalid portal target");}
        keys.clear();for(var object:interactions){unique(keys,object.id());inside(object.tile(),width,height);Objects.requireNonNull(object.kind());if(!identifier(object.action())||!identifier(object.sprite()))throw new IllegalArgumentException("Invalid interaction binding");}
    }
    private static boolean identifier(String value){return value!=null&&value.matches("[a-z][a-z0-9_]*");}
    private static void unique(Set<String> ids,String id){if(!identifier(id)||!ids.add(id))throw new IllegalArgumentException("Invalid/duplicate map object ID");}
    private static void inside(Tile tile,int width,int height){if(tile==null||tile.x()<0||tile.y()<0||tile.x()>=width||tile.y()>=height)throw new IllegalArgumentException("Tile outside map");}
    private static void walkable(Tile tile,int width,int height,List<Boolean> blocked){inside(tile,width,height);if(blocked.get(tile.y()*width+tile.x()))throw new IllegalArgumentException("Blocked spawn/portal");}
    public boolean contains(Tile tile){return tile!=null&&tile.x()>=0&&tile.y()>=0&&tile.x()<width&&tile.y()<height;}
    public boolean walkable(Tile tile){return contains(tile)&&!blocked.get(tile.y()*width+tile.x());}
}
