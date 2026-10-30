package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** Deterministic combat. Invalid actions never tick effects/cooldowns or spend a turn. */
public final class CombatService {
    public record Intent(String name,String description,int bonus,boolean strike,boolean defend) {}
    private final InventoryService inventory=new InventoryService();
    private static String pattern(Enemy enemy){var data=WorldContent.MONSTERS.get(enemy.getId());return data==null?"normal":data.pattern();}
    private static boolean phaseEnemy(Enemy enemy){return pattern(enemy).equals("warden")||pattern(enemy).equals("captain");}
    public static boolean storyMode(GameState state){return state.getCampaign().getDifficulty().equals("STORY");}
    public static Intent intent(GameState state){
        var enemy=state.getCampaign().getEnemy();if(enemy==null)return new Intent("","",0,false,false);
        int turn=state.getCampaign().getTurn()+1;
        if(turn%3==0)return new Intent("Heavy attack","A charged strike. Guard, stun or shield can help.",8,true,false);
        if(pattern(enemy).equals("wolf")&&turn%3==2)return new Intent("Charge","The wolf charges; no direct attack this round.",0,false,false);
        if(pattern(enemy).equals("guardian")&&turn%4==0)return new Intent("Defensive stance","The guardian raises a 16-point shield for two rounds after your action.",0,false,true);
        boolean phase=phaseEnemy(enemy)&&enemy.getHp()<=enemy.getMaxHp()/2;
        return new Intent(phase?(pattern(enemy).equals("captain")?"Drowned fury":"Crown fury"):"Counterattack",phase?(pattern(enemy).equals("captain")?"Phase II: direct attacks gain 5 damage; heavy attacks inflict Poison.":"Phase II: direct attacks gain 5 damage; heavy attacks inflict Burn."):"A normal direct counterattack.",0,true,false);
    }
    public static int cooldown(GameState state,String id){return state.getCampaign().getCooldowns().getOrDefault(id,0);}
    public static String unavailable(GameState state,String action){
        var hero=state.getCharacter();var skill=SkillCatalog.find(state,action);
        int cost=skill!=null?skill.mp():action.equals("spell")?10:action.equals("heal")?8:0;
        if(action.equals("companion")){
            var c=BondService.companion(state.getCampaign().getCompanion());
            if(c==null||!BondService.recruited(state,c))return "No active companion.";
            if(c.id().equals("mira")&&hero.getHp()>=hero.getMaxHp())return "HP is full. Companion support was not spent.";
        }
        if(cooldown(state,action)>0)return "Cooldown: "+cooldown(state,action)+" other actions remaining.";
        if(hero.getMp()<cost)return "Not enough MP. Guard to recover mana; the enemy waits.";
        if((action.equals("heal")||action.equals("rally"))&&hero.getHp()>=hero.getMaxHp())return "HP is full. No resource or turn was spent.";
        if(Set.of("potion","mana","greater","focus").contains(action)){
            String item=switch(action){case "potion"->"small_potion";case "mana"->"mana_potion";case "greater"->"greater_potion";default->"focus_draught";};
            if(!ItemCatalog.owns(state,item))return "No "+ItemCatalog.equippedName(item)+" remains. The enemy waits.";
            if(ItemCatalog.entry(item).hp()>0?hero.getHp()>=hero.getMaxHp():hero.getMp()>=hero.getMaxMp())return "That resource is already full. Item kept; the enemy waits.";
        }
        return "";
    }
    public List<Choice> choices(GameState state){
        List<Choice> choices=new ArrayList<>();choices.add(new Choice("Attack","attack"));
        for(var skill:SkillCatalog.forClass(state.getCharacter().getCharacterClass())){
            String label=skill.name()+" / "+skill.mp()+" MP";
            if(cooldown(state,skill.id())>0)label+=" / CD "+cooldown(state,skill.id());
            choices.add(new Choice(label,skill.id()));
        }
        choices.addAll(List.of(new Choice("Arcane Bolt / 10 MP","spell"),new Choice("Guard / recover 6 MP","guard"),
                new Choice("Mend / 8 MP"+(cooldown(state,"heal")>0?" / CD "+cooldown(state,"heal"):""),"heal"),
                new Choice("Drink HP potion","potion"),new Choice("Drink mana draught","mana"),new Choice("Retreat / lose up to 5 gold","flee")));
        if(ItemCatalog.owns(state,"greater_potion"))choices.add(new Choice("Greater Potion / 65 HP","greater"));
        if(ItemCatalog.owns(state,"focus_draught"))choices.add(new Choice("Focus Draught / 45 MP","focus"));
        if(!state.getCampaign().getCompanion().isBlank())choices.add(new Choice("Companion support / CD 3","companion"));
        return choices;
    }
    public static void clear(GameState state){var w=state.getCampaign();w.getPlayerEffects().clear();w.getEnemyEffects().clear();w.getCooldowns().clear();w.setDialogueNode("");w.setTurn(0);w.setChallenge("");}
    public void begin(GameState state,Enemy enemy){clear(state);state.getCampaign().setEnemy(enemy);}
    public static void applyEffect(Map<String,StatusEffect> effects,String id,int turns,int potency){
        var previous=effects.get(id);
        effects.put(id,new StatusEffect(Math.max(turns,previous==null?0:previous.getTurns()),Math.max(potency,previous==null?0:previous.getPotency())));
    }
    private void effect(Map<String,StatusEffect> effects,String id,int extra){var definition=SkillCatalog.effect(id);applyEffect(effects,id,definition.duration(),definition.potency()+extra);}
    private int absorb(Map<String,StatusEffect> effects,int damage){
        var shield=effects.get("shield");if(shield==null)return damage;
        int absorbed=Math.min(shield.getPotency(),damage);shield.setPotency(shield.getPotency()-absorbed);
        if(shield.getPotency()==0)effects.remove("shield");return damage-absorbed;
    }
    private int dot(Map<String,StatusEffect> effects,boolean reduced,String who,StringBuilder text){
        int total=0;
        for(String id:List.of("burn","poison")){
            var effect=effects.get(id);if(effect==null)continue;
            int damage=reduced?(int)Math.ceil(effect.getPotency()*.65):effect.getPotency();total+=damage;
            text.append(who).append(" suffers ").append(damage).append(" ").append(id).append(" damage. ");
        }
        return total;
    }
    private void tick(Map<String,StatusEffect> effects){effects.values().forEach(effect->effect.setTurns(effect.getTurns()-1));effects.values().removeIf(effect->effect.getTurns()<=0);}
    private void cooldowns(GameState state){var cds=state.getCampaign().getCooldowns();cds.replaceAll((id,turns)->turns-1);cds.values().removeIf(turns->turns<=0);}
    private void setCooldown(GameState state,String id,int turns){if(turns>0)state.getCampaign().getCooldowns().put(id,turns+1);}

    public void resolve(GameState state,String action,StoryResponse response){
        var world=state.getCampaign();var enemy=world.getEnemy();var hero=state.getCharacter();
        String blocked=unavailable(state,action);if(!blocked.isBlank()){response.setNarration(blocked);return;}
        if(action.equals("flee")){
            world.setEnemy(null);clear(state);response.setGoldChange(-Math.min(storyMode(state)?2:5,hero.getGold()));
            camp(state,response);response.setNarration("You retreat to camp, losing up to "+(storyMode(state)?2:5)+" gold. The enemy remains undefeated and will recover.");return;
        }
        Intent selected=intent(state);boolean wasPhase=enemy.getHp()<=enemy.getMaxHp()/2;
        int hp=hero.getHp(),mp=hero.getMp(),damage=0;boolean guard=false;
        StringBuilder text=new StringBuilder();var skill=SkillCatalog.find(state,action);
        if(skill!=null){
            mp-=skill.mp();int rank=SkillCatalog.rank(state,action);setCooldown(state,action,skill.cooldown());
            switch(skill.kind()){
                case "POWER"->damage=Math.max(1,ItemCatalog.attack(state)*2-enemy.getDefense()+4*rank);
                case "SHADOW"->damage=Math.max(1,ItemCatalog.attack(state)+hero.getAgility()-enemy.getDefense()/2+4*rank);
                case "EMBER"->{damage=hero.getIntelligence()*2+hero.getLevel()*2+3*rank;effect(world.getEnemyEffects(),"burn",0);}
                case "BASH"->{damage=Math.max(1,ItemCatalog.attack(state)-enemy.getDefense()+3*rank);effect(world.getEnemyEffects(),"stun",0);}
                case "FROST"->{damage=hero.getIntelligence()+3*rank;effect(world.getEnemyEffects(),"stun",0);}
                case "VENOM"->{damage=Math.max(1,ItemCatalog.attack(state)/2-enemy.getDefense()/2);effect(world.getEnemyEffects(),"poison",2*rank);}
                case "RALLY"->{int restored=Math.min(hero.getMaxHp()-hp,20+hero.getIntelligence()+5*rank);hp+=restored;text.append("Restored ").append(restored).append(" HP. ");}
                case "EVASION"->applyEffect(world.getPlayerEffects(),"shield",SkillCatalog.effect("shield").duration(),18+4*rank);
                case "WARD"->effect(world.getPlayerEffects(),"shield",4*rank);
                default->throw new IllegalStateException("Unknown skill kind");
            }
            text.append(skill.name()).append("! ");
        }else switch(action){
            case "companion"->{
                var ally=BondService.companion(world.getCompanion());int power=world.has("bond_"+ally.id())?12:8;
                setCooldown(state,"companion",3);
                switch(ally.id()){
                    case "mira"->hp=Math.min(hero.getMaxHp(),hp+power);
                    case "sera"->damage=power;
                    case "orin"->applyEffect(world.getPlayerEffects(),"shield",2,power);
                }
                text.append(ally.name()).append(" supports you! ");
            }
            case "attack"->{
                boolean critical=ItemCatalog.criticalEligible(state)&&(world.getTurn()+1)%ItemCatalog.criticalPeriod(state)==0;
                damage=Math.max(1,ItemCatalog.attack(state)-enemy.getDefense());if(critical)damage+=hero.getAgility()/2;
                text.append(critical?"Critical strike! ":"You strike. ");
            }
            case "spell"->{mp-=10;damage=hero.getIntelligence()*2+12;text.append("Arcane Bolt ignores armor. ");}
            case "guard"->{guard=true;mp=Math.min(hero.getMaxMp(),mp+6);text.append("You Guard and recover mana. ");}
            case "heal"->{mp-=8;hp=Math.min(hero.getMaxHp(),hp+25+hero.getIntelligence());setCooldown(state,"heal",2);text.append("Mend restores health. ");}
            case "potion","mana","greater","focus"->{
                String item=switch(action){case "potion"->"small_potion";case "mana"->"mana_potion";case "greater"->"greater_potion";default->"focus_draught";};inventory.remove(state.getInventory(),item);
                var entry=ItemCatalog.entry(item);hp=Math.min(hero.getMaxHp(),hp+entry.hp());mp=Math.min(hero.getMaxMp(),mp+entry.mp());
                text.append("You drink ").append(ItemCatalog.equippedName(item)).append(". ");
            }
            default->{response.setNarration("Choose a combat action.");return;}
        }
        damage=absorb(world.getEnemyEffects(),damage);enemy.setHp(Math.max(0,enemy.getHp()-damage));
        if(damage>0)text.append(enemy.getName()).append(" takes ").append(damage).append(" damage. ");
        if(enemy.getHp()==0){victory(state,response,enemy,text);finishDelta(hero,hp,mp,response,text);return;}
        int turn=world.getTurn()+1;world.setTurn(turn);
        if(world.getEnemyEffects().containsKey("stun"))text.append(enemy.getName()).append(" is stunned and cannot act. ");
        else if(selected.defend()){
            applyEffect(world.getEnemyEffects(),"shield",SkillCatalog.effect("shield").duration()+1,16); // Tick below leaves two full future rounds.
            text.append("The foe raises a 16-point shield for two rounds. ");
        }else if(selected.strike()){
            int phaseBonus=phaseEnemy(enemy)&&wasPhase?5:0;
            int fatigue=Math.max(0,turn-12)/2;
            int incoming=Math.max(1,enemy.getAttack()+selected.bonus()+phaseBonus+fatigue-ItemCatalog.defense(state));
            if(storyMode(state))incoming=(int)Math.ceil(incoming*.65);
            if(guard)incoming=Math.max(1,incoming/2);
            incoming=absorb(world.getPlayerEffects(),incoming);hp-=incoming;
            text.append(selected.name()).append("! You take ").append(incoming).append(" damage. ");
            if(phaseEnemy(enemy)&&wasPhase&&selected.bonus()>0)applyEffect(world.getPlayerEffects(),enemy.getId().equals("drowned_captain")?"poison":"burn",2,4);
            if(fatigue>0)text.append("Battle fatigue adds ").append(fatigue).append(" enemy power. ");
        }else text.append("The wolf charges its next heavy attack. ");
        hp-=dot(world.getPlayerEffects(),storyMode(state),hero.getName(),text);
        enemy.setHp(Math.max(0,enemy.getHp()-dot(world.getEnemyEffects(),false,enemy.getName(),text)));
        tick(world.getPlayerEffects());tick(world.getEnemyEffects());cooldowns(state);
        if(hp<=0){
            world.setEnemy(null);clear(state);world.mark("fallen","Camp scouts brought you home after a defeat.");
            hp=Math.max(1,hero.getMaxHp()/2);mp=Math.max(1,hero.getMaxMp()/2);
            int penalty=storyMode(state)?10:20;response.setGoldChange(-hero.getGold()*penalty/100);camp(state,response);
            text.append("You fall. Scouts return you to camp with half HP/MP; ").append(penalty).append("% gold was lost. No victory reward.");
        }else if(enemy.getHp()==0)victory(state,response,enemy,text);
        else if(phaseEnemy(enemy)&&!wasPhase&&enemy.getHp()<=enemy.getMaxHp()/2)text.append("The Crown fractures. Phase II begins next round!");
        finishDelta(hero,hp,mp,response,text);
    }
    private void finishDelta(com.rpgchatgame.model.Character hero,int hp,int mp,StoryResponse response,StringBuilder text){
        response.setHpChange(hp-hero.getHp());response.setMpChange(mp-hero.getMp());response.setNarration(text.toString());
    }
    private void camp(GameState state,StoryResponse response){state.setCurrentLocation("Survivor Camp");state.getCampaign().getVisited().add("Survivor Camp");response.setNewLocation("Survivor Camp");}
    private void victory(GameState state,StoryResponse response,Enemy enemy,StringBuilder text){
        var world=state.getCampaign();String challenge=world.getChallenge();world.setEnemy(null);clear(state);
        if(!challenge.isBlank()){
            boolean first=world.mark("challenge_"+challenge,"Cleared arena: "+challenge+".");
            if(first)response.getItemsGained().add(ItemCatalog.stack(challenge.equals("ash")?"ember_shard":challenge.equals("ice")?"frost_thread":"tide_pearl",2));
            text.append(first?"Arena victory! First-clear crafting supplies awarded. ":"Arena victory! Replay: no additional rewards. ");return;
        }
        if(!world.mark(enemy.getId(),"Defeated "+enemy.getName()+"."))return;
        var data=WorldContent.MONSTERS.get(enemy.getId());
        if(data==null)throw new IllegalStateException("Missing encounter definition");
        response.setExpGain(data.exp());response.setGoldChange(data.gold());
        for(var item:data.items())response.getItemsGained().add(ItemCatalog.stack(item.id(),item.quantity()));
        for(String flag:data.flags())world.mark(flag,"");WorldService.reputation(state,data.reputation());world.getCodex().add("enemy:"+enemy.getId());
        text.append("Victory! ").append(enemy.getName()).append(" is defeated. Your journal records the outcome.");
    }
    public static String effects(Map<String,StatusEffect> effects){
        if(effects.isEmpty())return "No active effects";
        return String.join(" / ",effects.entrySet().stream().map(e->com.rpgchatgame.util.UiText.tr(SkillCatalog.effect(e.getKey()).name()+": "+e.getValue().getPotency()+" / "+e.getValue().getTurns()+" rounds")).toList());
    }
}
