package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** Offline campaign with explicit action IDs, gated quests, and deterministic turn combat. */
public class LocalStoryGeneratorService implements StoryGeneratorService {
    public static final List<String> LOCATIONS = WorldContent.LOCATIONS.values().stream().map(WorldContent.Location::name).toList();
    private final WorldService expansion = new WorldService();
    private final CombatService combat=new CombatService();
    private final InventoryService inventory = new InventoryService();
    private static Choice c(String label,String action) { return new Choice(ActionDefinitions.label(action,label),action); }

    public List<Choice> choices(GameState state) {
        var world=state.getCampaign();
        if (world.epilogueClosed()) return List.of(c("Reflect on your journey", "look"), c("Explore the borderlands", "explore_borderlands"));
        if (world.getEnemy()!=null) return combat.choices(state);
        if (!world.getDialogueNode().isBlank()) {var replies=new ArrayList<>(expansion.choices(state));replies.addAll(LivingService.choices(state));return replies;}
        List<Choice> actions=new ArrayList<>();
        switch(state.getCurrentLocation()) {
            case "Burned Village" -> {
                if(!world.has("supplies"))actions.add(c("Search the burned homes", "search"));
                if(!world.has("survivors"))actions.add(c("Rescue Mira and the survivors", "rescue"));
            }
            case "Northern Gate" -> {
                if(!world.has("wolf"))actions.add(c("Confront the Ash Wolf", "fight_wolf"));
            }
            case "Emberwood Trail" -> {
                if(!world.has("herbs"))actions.add(c("Gather moonroot herbs", "herbs"));
            }
            case "Survivor Camp" -> {
                actions.add(c("Rest / 8 gold, first stay free", "rest"));
                if(world.has("survivors")&&!world.has("mira_story"))actions.add(c("Ask Mira about the Crown", "talk_mira"));
                if(world.has("survivors")&&ItemCatalog.owns(state,"silver_locket")&&!world.has("locket_returned"))actions.add(c("Return Mira's silver locket", "return_locket"));
                actions.add(c("Study your skills and supplies", "look"));
            }
            case "Emberwood Shrine" -> {
                if(!world.has("runes"))actions.add(c("Decipher the shrine's runes", "runes"));
                if(!world.has("guardian"))actions.add(c("Challenge the Hollow Guardian", "fight_guardian"));
                if(world.has("guardian")&&!world.has("sigil"))actions.add(c("Restore the Sun Sigil", "sigil"));
            }
            case "Sunken Ruins" -> {
                if(!world.has("archive"))actions.add(c("Read the last keeper's archive", "archive"));
                if(!world.has("locket"))actions.add(c("Search the flooded quarters", "locket"));
            }
            case "Cinder Tower" -> {
                if(!world.has("warden"))actions.add(c("Face the Cinder Warden", "fight_warden"));
                else {
                    if(world.has("sigil")&&world.has("archive"))actions.add(c("Purify the Crown / Sun Sigil", "end_purify"));
                    actions.add(c("Seal the Crown beneath the tower", "end_seal"));
                    actions.add(c("Claim the Crown's power", "end_claim"));
                }
            }
            default -> { }
        }
        actions.removeIf(choice -> choice.getAction().startsWith("go_") || (!world.getEnding().isBlank() && choice.getAction().startsWith("end_")));
        actions.addAll(LivingService.choices(state));actions.addAll(expansion.choices(state));actions.addAll(WorldService.routes(state));
        return actions;
    }

    public static String skillName(GameState state) {
        return switch(state.getCharacter().getCharacterClass()) {
            case "Rogue" -> "Shadow Strike";
            case "Mage" -> "Ember Surge";
            default -> "Power Strike";
        };
    }

    /** Only registered choice IDs are executed; the game no longer accepts custom chat. */
    public String resolve(GameState state,String input) { return input==null?"":input.trim().toLowerCase(Locale.ROOT); }

    @Override public StoryResponse generateNextScene(GameState state,String input) {
        String action=resolve(state,input);
        StoryResponse response=new StoryResponse();
        response.setNarration("That action is not available here. Choose a listed action. Unknown actions grant no rewards.");
        boolean offered=choices(state).stream().anyMatch(choice->choice.getAction().equals(action));
        if (action.equals("look")) {
            response.setNarration(describe(state));
        } else if (offered) {
            if(state.getCampaign().getEnemy()!=null)combat.resolve(state,action,response);
            else if (!LivingService.handle(state,action,response)&&!expansion.handle(state,action,response)) explore(state,action,response);
        }
        LivingService.comment(state,action,response);
        response.setChoices(choices(state));
        return response;
    }

    public String describe(GameState state) {
        if(state.getCampaign().epilogueClosed())return "Your journey is complete: "+state.getCampaign().getEnding()+". Your journal preserves the people and places you changed. Start a new story from Home to try another ending.";
        var enemy=state.getCampaign().getEnemy();
        if(enemy!=null)return enemy.getName()+" blocks your path. Read the intent, status effects and cooldowns. Guard halves direct damage and restores 6 MP. Invalid actions spend no turn. Fatigue increases enemy power after turn 12.";
        if (!state.getCampaign().getDialogueNode().isBlank()) return WorldContent.DIALOGUES.get(state.getCampaign().getDialogueNode()).text();
        return WorldContent.location(state.getCurrentLocation()).description()+(state.getCurrentLocation().equals("Survivor Camp")?"\n\n"+LivingService.campDescription(state):"");
    }

    private void once(GameState state,StoryResponse response,String flag) {
        var event=WorldContent.EVENTS.get(flag);
        if(event==null)throw new IllegalStateException("Missing campaign event: "+flag);
        if(state.getCampaign().mark(flag,event.text())) {
            response.setNarration(event.text());response.setExpGain(event.exp());response.setGoldChange(event.gold());WorldService.reputation(state,event.reputation());
        }
    }
    private void explore(GameState state,String action,StoryResponse response) {
        var world=state.getCampaign();var hero=state.getCharacter();
        switch(action) {
            case "search" -> {
                once(state,response,"supplies");
                response.getItemsGained().add(ItemCatalog.stack("small_potion",1));
            }
            case "rescue" -> once(state,response,"survivors");
            case "talk_mira" -> once(state,response,"mira_story");
            case "herbs" -> {
                once(state,response,"herbs");
                response.getItemsGained().add(ItemCatalog.stack("mana_potion",2));
            }
            case "runes" -> once(state,response,"runes");
            case "sigil" -> {
                if(!world.has("runes")) {response.setNarration("Decipher the runes first; guessing at the altar could destroy its last seal.");break;}
                once(state,response,"sigil");
                response.getItemsGained().add(ItemCatalog.stack("sun_sigil",1));
            }
            case "archive" -> once(state,response,"archive");
            case "locket" -> {
                once(state,response,"locket");
                response.getItemsGained().add(ItemCatalog.stack("silver_locket",1));
            }
            case "return_locket" -> {
                once(state,response,"locket_returned");
                inventory.remove(state.getInventory(),"silver_locket");
                if(!ItemCatalog.owns(state,"ranger_armor"))response.getItemsGained().add(ItemCatalog.stack("ranger_armor",1));
            }
            case "rest" -> {
                boolean free=!world.has("first_rest");
                boolean paid=free||hero.getGold()>=8;
                response.setHpChange(paid?hero.getMaxHp()-hero.getHp():Math.min(hero.getMaxHp()-hero.getHp(),hero.getMaxHp()/2));
                response.setMpChange(paid?hero.getMaxMp()-hero.getMp():Math.min(hero.getMaxMp()-hero.getMp(),hero.getMaxMp()/2));
                response.setGoldChange(free||!paid?0:-8);
                world.mark("first_rest", "You found shelter at Survivor Camp.");
                response.setNarration(paid?"You sleep beside the campfire. HP and MP are fully restored. "+(free?"The first stay is on the house.":"You pay 8 gold for food and shelter."):"The camp cannot spare a bed, but a rough rest restores up to half your maximum HP and MP for free.");
            }
            case "fight_wolf" -> begin(state,response,monster("wolf"));
            case "fight_guardian" -> begin(state,response,monster("guardian"));
            case "fight_warden" -> begin(state,response,monster("warden"));
            case "end_purify" -> finish(state,response,"Dawnkeeper", "You offer the Sun Sigil and speak the keeper's oath. The Crown's fire turns gold. The Warden's last breath becomes a warm wind, and the survivors find their way home beneath the first clear sunrise.");
            case "end_seal" -> finish(state,response,"The Silent Watch", "You seal the Crown beneath the tower. The village is safe, though a red light still pulses under the stone. You become its watcher, keeping a promise no monument will remember.");
            case "end_claim" -> finish(state,response,"Sovereign of Ash", "You take the Crown. Fire obeys, and the village rises from its ashes in a single night. Yet its people whisper your name with fear. The old keeper's story has found a new beginning.");
            default -> response.setNarration(describe(state));
        }
        response.setQuestUpdate(quest(state));
    }

    private Enemy monster(String id) {var data=WorldContent.MONSTERS.get(id);return new Enemy(data.id(),data.name(),data.hp(),data.attack(),data.defense());}
    private void begin(GameState state,StoryResponse response,Enemy enemy) {
        combat.begin(state,enemy);state.getCampaign().getCodex().add("enemy:"+enemy.getId());
        response.setNarration(enemy.getName()+" steps into your path. Combat begins. Strike, use your class skill, or Guard to recover mana. Read the enemy intent before each action; every third turn is heavy.");
    }
    private void finish(GameState state,StoryResponse response,String ending,String text) {
        state.getCampaign().setEnding(ending);
        state.getCampaign().mark("ending_"+ending,text);
        response.setNarration("ENDING / "+ending+"\n\n"+text);
    }
    public String quest(GameState state) {
        var world=state.getCampaign();
        if(!world.getEnding().isBlank())return "Completed / "+world.getEnding();
        if(world.has("warden"))return "Choose the fate of the Cinder Crown";
        if(world.has("guardian"))return "Read the archive and confront the Cinder Warden";
        if(world.has("wolf"))return "Uncover the shrine's secret and defeat its guardian";
        return "Save the survivors and clear the Northern Gate";
    }

}
