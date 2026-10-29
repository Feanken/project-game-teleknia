package com.rpgchatgame.service;

import com.rpgchatgame.model.Choice;
import com.rpgchatgame.model.GameState;
import com.rpgchatgame.model.InventoryItem;
import com.rpgchatgame.model.Item;
import com.rpgchatgame.model.StoryResponse;

import java.util.List;
import java.util.Locale;

public class LocalStoryGeneratorService implements StoryGeneratorService {
    @Override
    public StoryResponse generateNextScene(GameState gameState, String playerAction) {
        String action = playerAction == null ? "" : playerAction.toLowerCase(Locale.ROOT);
        if (action.contains("inventory")) {
            return inventoryHint();
        }
        if (action.contains("survivor") || action.contains("search") || action.contains("explore")) {
            return searchVillage();
        }
        if (action.contains("gate") || action.contains("north") || action.contains("inspect")) {
            return inspectGate();
        }
        if (action.contains("fight") || action.contains("attack") || action.contains("roar")) {
            return fightAshWolf();
        }
        if (action.contains("map") || action.contains("track") || action.contains("trail")) {
            return followTrail();
        }
        return cautiousCustomAction(playerAction);
    }

    private StoryResponse inventoryHint() {
        StoryResponse response = baseResponse();
        response.setNarration("You take a breath and check your satchel. The Rusty Sword feels crude but dependable, while the Old Map shows a faded mark beyond the northern gate.");
        response.setChoices(List.of(
                new Choice("Use the Old Map", "Use the Old Map to find a trail"),
                new Choice("Prepare for a fight", "Prepare to fight anything near the northern gate"),
                new Choice("Return to the village square", "Search the village for survivors")
        ));
        return response;
    }

    private StoryResponse searchVillage() {
        StoryResponse response = baseResponse();
        response.setNarration("You move through the burned homes and hear a weak knock beneath a collapsed stall. After pulling away blackened planks, you find a hidden coin pouch and claw marks pointing north.");
        response.setGoldChange(8);
        response.setQuestUpdate("Follow the claw marks toward the northern gate");
        response.setChoices(List.of(
                new Choice("Follow the claw marks", "Follow the claw marks to the northern gate"),
                new Choice("Search deeper", "Explore the village for supplies"),
                new Choice("Study the Old Map", "Use the Old Map to find a trail")
        ));
        return response;
    }

    private StoryResponse inspectGate() {
        StoryResponse response = baseResponse();
        response.setNarration("At the northern gate, ash drifts like snow. A wounded ash wolf lunges from the smoke. You raise your sword in time, but its claws rake your shoulder before it retreats.");
        response.setHpChange(-12);
        response.setExpGain(20);
        response.setNewLocation("Northern Gate");
        response.setChoices(List.of(
                new Choice("Counterattack", "Fight the ash wolf"),
                new Choice("Track it carefully", "Track the ash wolf trail"),
                new Choice("Fall back and recover", "Check your inventory")
        ));
        return response;
    }

    private StoryResponse fightAshWolf() {
        StoryResponse response = baseResponse();
        response.setNarration("You step into the smoke and meet the beast head-on. Steel flashes, the wolf howls, and your final strike drives it away into the dark woods. In the ashes, you find a cracked fang still warm with strange magic.");
        response.setHpChange(-8);
        response.setExpGain(45);
        response.setGoldChange(5);
        response.getItemsGained().add(new InventoryItem(new Item(
                "ash_fang",
                "Ash Fang",
                Item.ItemType.QUEST,
                Item.Rarity.UNCOMMON,
                "A warm fang taken from an ash wolf near the northern gate.",
                "Quest clue"
        ), 1));
        response.setQuestUpdate("Discover why ash wolves attacked the village");
        response.setNewLocation("Northern Gate");
        response.setChoices(List.of(
                new Choice("Follow the beast's trail", "Track the ash wolf trail"),
                new Choice("Return to survivors", "Search the village for survivors"),
                new Choice("Inspect the fang", "Inspect the Ash Fang")
        ));
        return response;
    }

    private StoryResponse followTrail() {
        StoryResponse response = baseResponse();
        response.setNarration("Using the Old Map, you match the claw marks to a forgotten hunter path. The trail bends toward the Emberwood Shrine, where a low red glow pulses between the trees.");
        response.setExpGain(25);
        response.setNewLocation("Emberwood Trail");
        response.setQuestUpdate("Reach Emberwood Shrine");
        response.setChoices(List.of(
                new Choice("Approach the shrine", "Approach Emberwood Shrine"),
                new Choice("Scout the trail", "Explore the trail for supplies"),
                new Choice("Set an ambush", "Prepare to fight anything near the shrine")
        ));
        return response;
    }

    private StoryResponse cautiousCustomAction(String playerAction) {
        StoryResponse response = baseResponse();
        String actionText = playerAction == null || playerAction.isBlank() ? "your next move" : playerAction;
        response.setNarration("You choose to " + actionText + ". The world answers quietly: embers shift, distant stones crack, and your instincts sharpen. You find a safe angle forward, though the mystery remains unresolved.");
        response.setExpGain(10);
        response.setChoices(List.of(
                new Choice("Search for clues", "Search the village for survivors"),
                new Choice("Move north", "Inspect the northern gate"),
                new Choice("Use the map", "Use the Old Map to find a trail")
        ));
        return response;
    }

    private StoryResponse baseResponse() {
        StoryResponse response = new StoryResponse();
        response.setChoices(List.of(
                new Choice("Search the village for survivors", "Search the village for survivors"),
                new Choice("Inspect the northern gate", "Inspect the northern gate"),
                new Choice("Check your inventory", "Check your inventory")
        ));
        return response;
    }
}
