package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** 1.5 systems: bounded milestone affinity, finite materials and isolated arena rewards. */
public final class BondService {
    public record Companion(String id,String name,String sprite,String gate,String role,String quest,List<String> bonds,String greeting) {}
    public record Recipe(String id,String output,String blueprint,String material,int amount,int gold) {}
    public record Challenge(String id,String name,String sprite,int hp,int attack,int defense,String modifier) {}
    public static final List<Companion> COMPANIONS=List.of(
        new Companion("mira","Mira","ilyra","survivors","Healer: restore 8 HP; bonded 12 HP","A Home Worth Returning To",List.of("locket_returned","q_medicine","survivors"),"I cannot undo the fire. But we can make sure nobody faces the next one alone."),
        new Companion("sera","Sera","sera","q_signal","Striker: 8 damage; bonded 12 damage","A Road Without Fear",List.of("q_signal","q_caravan","rime_resolved"),"The signal burns again. Walk the pass with me, and let us bring every traveler home."),
        new Companion("orin","Orin","orin","q_oath","Protector: 8 shield; bonded 12 shield","The Living Oath",List.of("q_oath","archive","q_chart"),"An oath means little carved in stone. Let us discover what it means among the living.")
    );
    public static final List<Recipe> RECIPES=List.of(
        new Recipe("potion","small_potion","wolf","ember_shard",1,3),
        new Recipe("mana","mana_potion","wolf","ember_shard",1,3),
        new Recipe("greater","greater_potion","q_signal","frost_thread",1,6),
        new Recipe("focus","focus_draught","q_chart","tide_pearl",1,6),
        new Recipe("blade","tempered_blade","guardian","ember_shard",3,15),
        new Recipe("ward","woven_ward","q_oath","frost_thread",3,15),
        new Recipe("charm","bond_charm","q_chart","tide_pearl",3,15),
        new Recipe("frost","awakened_frost","rime_resolved","frost_thread",4,20)
    );
    public static final List<Challenge> CHALLENGES=List.of(
        new Challenge("ash","Trial of Ash","field_wolf",100,20,5,"Fury: +4 enemy attack; 10 gold entry."),
        new Challenge("ice","Trial of Ice","rime_sentinel",150,20,12,"Bulwark: +7 enemy defense; 10 gold entry."),
        new Challenge("tide","Trial of Tides","drowned_captain",180,24,7,"Attrition: +80 enemy HP; 10 gold entry.")
    );
    public static Companion companion(String id){return COMPANIONS.stream().filter(c->c.id().equals(id)).findFirst().orElse(null);}
    public static int affinity(GameState state,Companion c){return (int)c.bonds().stream().filter(state.getCampaign()::has).count();}
    public static boolean recruited(GameState s,Companion c){return s.getCampaign().has("recruit_"+c.id());}
    public static int quantity(GameState s,String id){return s.getInventory().stream().filter(i->i.getItem().getId().equals(id)).mapToInt(InventoryItem::getQuantity).sum();}
    public static boolean camp(GameState s){return s.getCurrentLocation().equals("Survivor Camp")&&s.getCampaign().getEnemy()==null&&!s.getCampaign().epilogueClosed()&&s.getCampaign().getDialogueNode().isBlank();}
    public static void prepare(GameState s){
        var w=s.getCampaign();var inv=new InventoryService();
        for(String milestone:List.of("wolf","guardian","warden","q_signal","q_oath","rime_resolved","q_chart","q_captain")){
            if(w.has(milestone)&&w.mark("material_"+milestone,"Crafting supplies delivered for "+milestone+".")){
                String material=milestone.startsWith("q_c")?"tide_pearl":List.of("q_signal","q_oath","rime_resolved").contains(milestone)?"frost_thread":"ember_shard";
                inv.addItem(s.getInventory(),ItemCatalog.stack(material,4));
            }
        }
    }
    public static String manage(GameState s,String action,String id){
        if(!camp(s))return "Visit camp outside combat and conversations; reopen the borderlands after an ending.";
        var w=s.getCampaign();var c=companion(id);
        if(action.equals("dismiss")){w.setCompanion("");return "Your companion rests at camp. No affinity is lost.";}
        if(c==null)return "Unknown companion.";
        if(action.equals("recruit")){
            if(!w.has(c.gate()))return "Complete milestone: "+c.gate()+".";
            if(!w.mark("recruit_"+id,c.name()+" joined your camp."))return "Already recruited.";
            return c.name()+": "+c.greeting();
        }
        if(!recruited(s,c))return "Recruit this companion first.";
        if(action.equals("activate")){w.setCompanion(id);return c.name()+" is your active companion. Only one ally accompanies you.";}
        if(action.equals("talk"))return c.name()+": "+(w.has("bond_"+id)?"Our promise lives in every step we take together.":c.greeting())+" Affinity "+affinity(s,c)+" / 3. "+c.quest()+": complete two relevant milestones, then resolve this personal quest at camp.";
        if(action.equals("bond")){
            if(affinity(s,c)<2)return "Personal quest requires two relevant milestones: "+String.join(", ",c.bonds());
            if(!w.mark("bond_"+id,c.quest()+": a promise kept. Support skill strengthened."))return "Personal quest already completed.";
            return c.name()+": "+switch(id){case "mira"->"We found more than a locket or medicine. We found people willing to stay. This camp is home.";case "sera"->"The road is safe because we chose to return for others. I will keep that light burning.";default->"I once guarded stones. Today I choose to protect the people who will tell our story.";};
        }
        return "Choose a companion action.";
    }
    public static String craft(GameState s,String id){
        if(!camp(s))return "Craft only at camp outside combat and conversations.";
        var r=RECIPES.stream().filter(x->x.id().equals(id)).findFirst().orElse(null);
        if(r==null)return "Unknown recipe.";
        if(!s.getCampaign().has(r.blueprint()))return "Blueprint requires milestone: "+r.blueprint();
        var item=ItemCatalog.entry(r.output()).item();
        if(item.getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(s,r.output()))return "You already own this crafted equipment.";
        if(quantity(s,r.material())<r.amount()||s.getCharacter().getGold()<r.gold())return "Insufficient materials or gold. Nothing consumed.";
        var inventory=new InventoryService();for(int i=0;i<r.amount();i++)inventory.remove(s.getInventory(),r.material());
        s.getCharacter().setGold(s.getCharacter().getGold()-r.gold());inventory.addItem(s.getInventory(),ItemCatalog.stack(r.output(),1));
        s.getCampaign().mark("craft_"+r.id(),"Crafted "+item.getName()+".");return "Crafted "+item.getName()+".";
    }
    public static String challenge(GameState s,String id){
        if(!camp(s)||s.getCampaign().getEnding().isBlank())return "Challenges require a completed campaign and a visit to camp after reopening the borderlands.";
        var c=CHALLENGES.stream().filter(x->x.id().equals(id)).findFirst().orElse(null);
        if(c==null)return "Unknown challenge.";
        if(s.getCharacter().getGold()<10)return "Entry requires 10 gold. No turn spent.";
        s.getCharacter().setGold(s.getCharacter().getGold()-10);
        new CombatService().begin(s,new Enemy("arena_"+id,c.name(),c.hp(),c.attack(),c.defense()));
        s.getCampaign().setChallenge(id);return c.name()+" / "+c.modifier()+" First clear: 2 crafting materials. Replays give no gold, EXP or unique story items. Ending stays unchanged.";
    }
    public static Set<String> achievements(GameState s){
        Set<String> result=new TreeSet<>();var w=s.getCampaign();
        if(!w.getEnding().isBlank())result.add("Ending / "+w.getEnding());
        if(w.has("locket_returned"))result.add("Achievement / A Kindness Remembered");
        if(w.getVisited().size()==WorldContent.LOCATIONS.size())result.add("Achievement / Roads Beyond Emberwood");
        if(COMPANIONS.stream().allMatch(c->recruited(s,c)))result.add("Achievement / Found Family");
        if(COMPANIONS.stream().allMatch(c->w.has("bond_"+c.id())))result.add("Achievement / Bonds and Embers");
        if(RECIPES.stream().allMatch(r->w.has("craft_"+r.id())))result.add("Achievement / Master Artisan");
        if(CHALLENGES.stream().allMatch(c->w.has("challenge_"+c.id())))result.add("Achievement / Arena Champion");
        return result;
    }
}
