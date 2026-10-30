package com.rpgchatgame.service;

import com.rpgchatgame.model.GameState;
import java.util.*;

/** One permanent build choice at a time; combat bonuses never alter maximum resources. */
public final class MasteryService {
    public record Path(String id,String name,String klass,String passive,String skill,int mp,int cooldown) {}
    public static final List<Path> PATHS=List.of(
        new Path("guardian","Guardian","Fighter","Direct incoming damage -3 (minimum 1).","Aegis",10,3),
        new Path("berserker","Berserker","Fighter","Attack and Power Strike gain 4 damage while at half HP or below.","Rending Blow",12,3),
        new Path("assassin","Assassin","Rogue","Every third battle turn: basic attack gains 6 damage.","Nightfall",12,3),
        new Path("ranger","Ranger","Rogue","Basic attacks gain 3 damage.","Thorn Volley",10,3),
        new Path("pyromancer","Pyromancer","Mage","Ember Surge burn gains 2 potency.","Wildfire",14,3),
        new Path("arcanist","Arcanist","Mage","Arcane Bolt costs 8 MP instead of 10.","Prismatic Lance",12,3));
    public static Path find(String id){return PATHS.stream().filter(p->p.id().equals(id)).findFirst().orElse(null);}
    public static Path active(GameState s){return find(s.getCampaign().getSpecialization());}
    public static boolean is(GameState s,String id){return s.getCampaign().getSpecialization().equals(id);}
    public static int cost(GameState s){return s.getCampaign().getSpecialization().isBlank()?0:30;}
    public static String blocked(GameState s,String id){
        Path p=find(id);
        if(p==null||!p.klass().equals(s.getCharacter().getCharacterClass()))return "Choose a specialization for your class.";
        if(is(s,id))return "This specialization is already active.";
        if(s.getCharacter().getLevel()<5||!s.getCampaign().has("guardian"))return "Requires level 5 and victory over the Hollow Guardian.";
        if(!BondService.camp(s))return "Visit Survivor Camp outside combat and conversations; reopen the borderlands after an ending.";
        if(s.getCharacter().getGold()<cost(s))return "Changing specialization requires 30 gold.";
        return "";
    }
    public static String choose(GameState s,String id){
        String reason=blocked(s,id);if(!reason.isBlank())return reason;
        int fee=cost(s);s.getCharacter().setGold(s.getCharacter().getGold()-fee);s.getCampaign().setSpecialization(id);
        return "Specialization: "+find(id).name()+" / "+fee+" gold. HP, MP and skill ranks unchanged.";
    }
    public static int damage(GameState s,Path p){var h=s.getCharacter();return switch(p.id()){
        case "guardian"->0;case "berserker"->ItemCatalog.attack(s)*2+8;
        case "assassin"->ItemCatalog.attack(s)+h.getAgility()+12;
        case "ranger"->ItemCatalog.attack(s)+h.getAgility();
        case "pyromancer"->h.getIntelligence()*2+8;default->h.getIntelligence()*2+10;};}
    public static String preview(GameState s,Path p){return switch(p.id()){
        case "guardian"->"Shield: 24 capacity / 2 rounds. Incoming damage still has a minimum of 1 before shield.";
        case "berserker"->"Power before armor: "+damage(s,p);
        case "assassin"->"Direct damage: "+damage(s,p);
        case "ranger"->"Power before half armor: "+damage(s,p)+" / Poison: 5 x 3 rounds";
        case "pyromancer"->"Direct damage: "+damage(s,p)+" / Burn: 6 x 3 rounds";
        default->"Direct damage: "+damage(s,p)+" / Shield: 8 capacity / 2 rounds";};}
    public static String pattern(String id){return switch(id){case "arena_crucible"->"scorcher";case "arena_mirror"->"sentinel";case "arena_gale"->"duelist";default->"normal";};}
}
