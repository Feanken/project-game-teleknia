package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import java.util.*;

/** Offline campaign with explicit action IDs, gated quests, and deterministic turn combat. */
public class LocalStoryGeneratorService implements StoryGeneratorService {
    public static final List<String> LOCATIONS = List.of("Burned Village", "Northern Gate", "Emberwood Trail", "Survivor Camp", "Emberwood Shrine", "Sunken Ruins", "Cinder Tower");
    private final CombatService combat=new CombatService();
    private final InventoryService inventory = new InventoryService();
    private static Choice c(String label,String action) { return new Choice(ActionDefinitions.label(action,label),action); }

    public List<Choice> choices(GameState state) {
        var world=state.getCampaign();
        if (!world.getEnding().isBlank()) return List.of(c("Reflect on your journey", "look"));
        if (world.getEnemy()!=null) return combat.choices(state);
        List<Choice> actions=new ArrayList<>();
        switch(state.getCurrentLocation()) {
            case "Burned Village" -> {
                if(!world.has("supplies"))actions.add(c("Search the burned homes", "search"));
                if(!world.has("survivors"))actions.add(c("Rescue Mira and the survivors", "rescue"));
                actions.add(c("Travel to Northern Gate", "go_gate"));
                if(world.has("survivors"))actions.add(c("Travel to Survivor Camp", "go_camp"));
            }
            case "Northern Gate" -> {
                if(!world.has("wolf"))actions.add(c("Confront the Ash Wolf", "fight_wolf"));
                else actions.add(c("Follow the trail into Emberwood", "go_trail"));
                actions.add(c("Return to the village", "go_village"));
                actions.add(c("Travel to Survivor Camp", "go_camp"));
            }
            case "Emberwood Trail" -> {
                if(!world.has("herbs"))actions.add(c("Gather moonroot herbs", "herbs"));
                actions.add(c("Approach Emberwood Shrine", "go_shrine"));
                actions.add(c("Rest at Survivor Camp", "go_camp"));
                actions.add(c("Return to Northern Gate", "go_gate"));
                if(world.has("guardian"))actions.add(c("Explore Sunken Ruins", "go_ruins"));
            }
            case "Survivor Camp" -> {
                actions.add(c("Rest / 8 gold, first stay free", "rest"));
                if(world.has("survivors")&&!world.has("mira_story"))actions.add(c("Ask Mira about the Crown", "talk_mira"));
                if(world.has("survivors")&&ItemCatalog.owns(state,"silver_locket")&&!world.has("locket_returned"))actions.add(c("Return Mira's silver locket", "return_locket"));
                actions.add(c("Return to Burned Village", "go_village"));
                actions.add(c("Travel to Northern Gate", "go_gate"));
                if(world.has("wolf"))actions.add(c("Travel to Emberwood Trail", "go_trail"));
                if(world.has("guardian"))actions.add(c("Travel to Sunken Ruins", "go_ruins"));
                actions.add(c("Study your skills and supplies", "look"));
            }
            case "Emberwood Shrine" -> {
                if(!world.has("runes"))actions.add(c("Decipher the shrine's runes", "runes"));
                if(!world.has("guardian"))actions.add(c("Challenge the Hollow Guardian", "fight_guardian"));
                if(world.has("guardian")&&!world.has("sigil"))actions.add(c("Restore the Sun Sigil", "sigil"));
                if(world.has("guardian"))actions.add(c("Descend to Sunken Ruins", "go_ruins"));
                actions.add(c("Return to Emberwood Trail", "go_trail"));
                actions.add(c("Travel to Survivor Camp", "go_camp"));
            }
            case "Sunken Ruins" -> {
                if(!world.has("archive"))actions.add(c("Read the last keeper's archive", "archive"));
                if(!world.has("locket"))actions.add(c("Search the flooded quarters", "locket"));
                actions.add(c("Ascend Cinder Tower", "go_tower"));
                actions.add(c("Return to the shrine", "go_shrine"));
                actions.add(c("Return to Survivor Camp", "go_camp"));
            }
            case "Cinder Tower" -> {
                if(!world.has("warden"))actions.add(c("Face the Cinder Warden", "fight_warden"));
                else {
                    if(world.has("sigil")&&world.has("archive"))actions.add(c("Purify the Crown / Sun Sigil", "end_purify"));
                    actions.add(c("Seal the Crown beneath the tower", "end_seal"));
                    actions.add(c("Claim the Crown's power", "end_claim"));
                }
                actions.add(c("Return to Sunken Ruins", "go_ruins"));
                actions.add(c("Return to Survivor Camp", "go_camp"));
            }
            default -> actions.add(c("Find the village road", "go_village"));
        }
        return actions;
    }

    public static String skillName(GameState state) {
        return switch(state.getCharacter().getCharacterClass()) {
            case "Rogue" -> "Shadow Strike";
            case "Mage" -> "Ember Surge";
            default -> "Power Strike";
        };
    }

    /** Free text is deliberately bounded: aliases resolve only to currently offered actions. */
    public String resolve(GameState state,String input) {
        String action=input==null?"":input.trim().toLowerCase(Locale.ROOT);
        return switch(action) {
            case "serang", "fight", "hit" -> "attack";
            case "bertahan", "defend" -> "guard";
            case "lari", "retreat" -> "flee";
            case "sembuh", "mend" -> "heal";
            case "istirahat", "sleep" -> "rest";
            case "lihat", "help", "bantuan", "inventory", "map", "journal" -> "look";
            case "search the village for survivors" -> "rescue";
            case "inspect the northern gate" -> "go_gate";
            default -> action;
        };
    }

    @Override public StoryResponse generateNextScene(GameState state,String input) {
        String action=resolve(state,input);
        StoryResponse response=new StoryResponse();
        response.setNarration("That action is not available here. Choose a listed action, or type help. Unknown actions grant no rewards.");
        boolean offered=choices(state).stream().anyMatch(choice->choice.getAction().equals(action));
        if (action.equals("look")) {
            response.setNarration(describe(state));
        } else if (offered) {
            if(state.getCampaign().getEnemy()!=null)combat.resolve(state,action,response);
            else explore(state,action,response);
        }
        response.setChoices(choices(state));
        return response;
    }

    public String describe(GameState state) {
        if(!state.getCampaign().getEnding().isBlank())return "Your journey is complete: "+state.getCampaign().getEnding()+". Your journal preserves the people and places you changed. Start a new story from Home to try another ending.";
        var enemy=state.getCampaign().getEnemy();
        if(enemy!=null)return enemy.getName()+" blocks your path. Read the intent, status effects and cooldowns. Guard halves direct damage and restores 6 MP. Invalid actions spend no turn. Fatigue increases enemy power after turn 12.";
        return switch(state.getCurrentLocation()) {
            case "Burned Village" -> "Smoke rises from the homes you once knew. Mira calls from beneath a fallen beam. Tracks lead north, but someone must tend to those left behind.";
            case "Northern Gate" -> state.getCampaign().has("wolf")?"The gate is quiet. Claw marks point toward the shrine in the Old Map.":"An ash wolf guards the northern road. Its eyes burn with a light that does not belong to it.";
            case "Emberwood Trail" -> "Moonroot grows between blackened trees. The Old Map reveals a path to the shrine, while camp lanterns glimmer to the west.";
            case "Survivor Camp" -> "A smith and a herbalist keep watch beside the campfire. Open Market to buy supplies. Your first rest is free; later rests cost 8 gold. If you cannot afford shelter, a free rough rest restores half your maximum HP and MP.";
            case "Emberwood Shrine" -> "A hollow knight kneels before a broken sun altar. The runes tell of a crown built to hold a wildfire, and a keeper who chose to wear it.";
            case "Sunken Ruins" -> "Water covers the old library's floor. A keeper's archive and Mira's missing locket wait among the ruined quarters.";
            case "Cinder Tower" -> "The Cinder Warden stands beneath a crown of living flame. The village burned when its ancient seal failed. Your last choice will decide what rises from those ashes.";
            default -> "Consult your journal and choose a road.";
        };
    }

    private void travel(GameState state,StoryResponse response,String location) {
        state.setCurrentLocation(location);state.getCharacter().setCurrentLocation(location);
        state.getCampaign().getVisited().add(location);
        response.setNewLocation(location);response.setNarration(describe(state));
    }
    private void once(GameState state,StoryResponse response,String flag,String text,int exp,int gold) {
        if(state.getCampaign().mark(flag,text)) {response.setNarration(text);response.setExpGain(exp);response.setGoldChange(gold);}
    }
    private void explore(GameState state,String action,StoryResponse response) {
        var world=state.getCampaign();var hero=state.getCharacter();
        switch(action) {
            case "go_village" -> travel(state,response,"Burned Village");
            case "go_gate" -> travel(state,response,"Northern Gate");
            case "go_trail" -> travel(state,response,"Emberwood Trail");
            case "go_camp" -> travel(state,response,"Survivor Camp");
            case "go_shrine" -> travel(state,response,"Emberwood Shrine");
            case "go_ruins" -> travel(state,response,"Sunken Ruins");
            case "go_tower" -> travel(state,response,"Cinder Tower");
            case "search" -> {
                once(state,response,"supplies","You salvage a coin pouch and sealed medicine from the burned homes. The supplies here are exhausted.",10,18);
                response.getItemsGained().add(ItemCatalog.stack("small_potion",1));
            }
            case "rescue" -> once(state,response,"survivors","You free Mira and two children from a collapsed house. They establish a camp west of Emberwood. Mira asks you to find her brother's silver locket.",25,12);
            case "talk_mira" -> once(state,response,"mira_story","Mira remembers a sun sigil above the shrine. Her brother followed the old keepers into the ruins. 'If the Crown can be healed, let our village be the last to burn.'",15,0);
            case "herbs" -> {
                once(state,response,"herbs","You harvest the last healthy moonroot and brew two mana draughts. The rest must be left to regrow.",15,0);
                response.getItemsGained().add(ItemCatalog.stack("mana_potion",2));
            }
            case "runes" -> once(state,response,"runes","The shrine's runes warn: the Warden was a protector, not a conqueror. Restore the Sun Sigil and learn the keeper's oath before touching the Crown.",25,0);
            case "sigil" -> {
                if(!world.has("runes")) {response.setNarration("Decipher the runes first; guessing at the altar could destroy its last seal.");break;}
                once(state,response,"sigil","You fit the altar's fragments together. The Sun Sigil answers with a pale dawn, gentle enough to hold in your hands.",25,0);
                response.getItemsGained().add(ItemCatalog.stack("sun_sigil",1));
            }
            case "archive" -> once(state,response,"archive","The last keeper's archive reveals the oath: offer the flame a home, never a throne. With the Sun Sigil, you may purify the Crown instead of simply burying it.",35,15);
            case "locket" -> {
                once(state,response,"locket","In the flooded quarters you find Mira's silver locket and a final note: her brother stayed behind to evacuate the keepers. His kindness outlived the fire.",20,12);
                response.getItemsGained().add(ItemCatalog.stack("silver_locket",1));
            }
            case "return_locket" -> {
                once(state,response,"locket_returned","Mira closes the locket around her neck. The camp shares its savings and armor with you. A small part of the village has come home.",30,30);
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
            case "fight_wolf" -> begin(state,response,new Enemy("wolf","Ash Wolf",38,14,2));
            case "fight_guardian" -> begin(state,response,new Enemy("guardian","Hollow Guardian",86,19,5));
            case "fight_warden" -> begin(state,response,new Enemy("warden","Cinder Warden",150,25,8));
            case "end_purify" -> finish(state,response,"Dawnkeeper", "You offer the Sun Sigil and speak the keeper's oath. The Crown's fire turns gold. The Warden's last breath becomes a warm wind, and the survivors find their way home beneath the first clear sunrise.");
            case "end_seal" -> finish(state,response,"The Silent Watch", "You seal the Crown beneath the tower. The village is safe, though a red light still pulses under the stone. You become its watcher, keeping a promise no monument will remember.");
            case "end_claim" -> finish(state,response,"Sovereign of Ash", "You take the Crown. Fire obeys, and the village rises from its ashes in a single night. Yet its people whisper your name with fear. The old keeper's story has found a new beginning.");
            default -> response.setNarration(describe(state));
        }
        response.setQuestUpdate(quest(state));
    }

    private void begin(GameState state,StoryResponse response,Enemy enemy) {
        combat.begin(state,enemy);
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
