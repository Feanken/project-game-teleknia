package com.rpgchatgame.service;

import com.rpgchatgame.model.GameState;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;

public final class SkillCatalog {
    public record Skill(String id,String name,String characterClass,int mp,int cooldown,String kind,String description) {}
    public record Effect(String id,String name,int duration,int potency,String description) {}
    private static final List<Skill> SKILLS=List.of(read("skills",Skill[].class));
    private static final List<Effect> EFFECTS=List.of(read("statuses",Effect[].class));
    static {
        for(String klass:List.of("Fighter","Rogue","Mage")){
            Set<String> ids=new HashSet<>();
            for(var skill:forClass(klass))if(!ids.add(skill.id())||skill.mp()<1||skill.cooldown()<0||skill.cooldown()>6||skill.description()==null)throw new IllegalStateException("Invalid skill definition");
            if(ids.size()!=3||!ids.contains("skill"))throw new IllegalStateException("Each class needs three skills");
        }
        if(EFFECTS.size()!=4)throw new IllegalStateException("Four status definitions required");
        Set<String> ids=new HashSet<>();
        for(var effect:EFFECTS)if(!ids.add(effect.id())||effect.duration()<1||effect.duration()>5||effect.potency()<0)throw new IllegalStateException("Invalid status definition");
    }
    private SkillCatalog() {}
    private static <T>T read(String name,Class<T> type){try(var input=SkillCatalog.class.getResourceAsStream("/definitions/"+name+".json")){
        if(input==null)throw new IllegalStateException("Missing combat definition: "+name);
        return JsonUtil.fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),type);
    }catch(java.io.IOException error){throw new ExceptionInInitializerError(error);}}
    public static List<Skill> forClass(String klass){return SKILLS.stream().filter(s->s.characterClass().equals(klass)).toList();}
    public static Skill find(GameState state,String id){return forClass(state.getCharacter().getCharacterClass()).stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
    public static Effect effect(String id){return EFFECTS.stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();}
    public static String preview(GameState state,Skill skill,int rank){
        var hero=state.getCharacter();
        return switch(skill.kind()){
            case "POWER"->"Power before armor: "+(ItemCatalog.attack(state)*2+4*rank);
            case "SHADOW"->"Power before half armor: "+(ItemCatalog.attack(state)+hero.getAgility()+4*rank);
            case "EMBER"->"Direct damage: "+(hero.getIntelligence()*2+hero.getLevel()*2+3*rank)+" / Burn: 4 x 3 rounds";
            case "BASH"->"Power before armor: "+(ItemCatalog.attack(state)+3*rank)+" / Stun: 1 round";
            case "FROST"->"Direct damage: "+(hero.getIntelligence()+3*rank)+" / Stun: 1 round";
            case "VENOM"->"Power before half armor: "+(ItemCatalog.attack(state)/2)+" / Poison: "+(5+2*rank)+" x 3 rounds";
            case "RALLY"->"Healing: "+(20+hero.getIntelligence()+5*rank)+" HP";
            case "EVASION"->"Shield: "+(18+4*rank)+" capacity / 2 rounds";
            case "WARD"->"Shield: "+(20+4*rank)+" capacity / 2 rounds";
            default->throw new IllegalStateException("Unknown skill kind");
        };
    }
    public static int rank(GameState state,String id){return state.getCampaign().getSkillRanks().getOrDefault(id,0);}
    public static int points(GameState state){return state.getCharacter().getLevel()-state.getCampaign().getSkillRanks().values().stream().mapToInt(Integer::intValue).sum();}
}
