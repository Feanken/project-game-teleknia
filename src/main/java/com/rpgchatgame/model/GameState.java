package com.rpgchatgame.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private CampaignState campaign = new CampaignState();
    public CampaignState getCampaign() { return campaign; }
    public void setCampaign(CampaignState value) { campaign = value; }
    private Character character;
    private List<InventoryItem> inventory = new ArrayList<>();
    private String currentStorySummary;
    private String currentLocation;
    private String currentQuest;
    private List<StoryLog> storyLogs = new ArrayList<>();
    private List<Choice> currentChoices = new ArrayList<>();

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public List<InventoryItem> getInventory() {
        return inventory;
    }

    public void setInventory(List<InventoryItem> inventory) {
        this.inventory = inventory;
    }

    public String getCurrentStorySummary() {
        return currentStorySummary;
    }

    public void setCurrentStorySummary(String currentStorySummary) {
        this.currentStorySummary = currentStorySummary;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getCurrentQuest() {
        return currentQuest;
    }

    public void setCurrentQuest(String currentQuest) {
        this.currentQuest = currentQuest;
    }

    public List<StoryLog> getStoryLogs() {
        return storyLogs;
    }

    public void setStoryLogs(List<StoryLog> storyLogs) {
        this.storyLogs = storyLogs;
    }

    public List<Choice> getCurrentChoices() {
        return currentChoices;
    }

    public void setCurrentChoices(List<Choice> currentChoices) {
        this.currentChoices = currentChoices;
    }
}
