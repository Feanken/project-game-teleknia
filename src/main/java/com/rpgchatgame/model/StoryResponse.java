package com.rpgchatgame.model;

import java.util.ArrayList;
import java.util.List;

public class StoryResponse {
    private String narration;
    private List<Choice> choices = new ArrayList<>();
    private int hpChange;
    private int mpChange;
    private int expGain;
    private int goldChange;
    private String newLocation;
    private String questUpdate;
    private List<InventoryItem> itemsGained = new ArrayList<>();

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public int getHpChange() {
        return hpChange;
    }

    public void setHpChange(int hpChange) {
        this.hpChange = hpChange;
    }

    public int getMpChange() {
        return mpChange;
    }

    public void setMpChange(int mpChange) {
        this.mpChange = mpChange;
    }

    public int getExpGain() {
        return expGain;
    }

    public void setExpGain(int expGain) {
        this.expGain = expGain;
    }

    public int getGoldChange() {
        return goldChange;
    }

    public void setGoldChange(int goldChange) {
        this.goldChange = goldChange;
    }

    public String getNewLocation() {
        return newLocation;
    }

    public void setNewLocation(String newLocation) {
        this.newLocation = newLocation;
    }

    public String getQuestUpdate() {
        return questUpdate;
    }

    public void setQuestUpdate(String questUpdate) {
        this.questUpdate = questUpdate;
    }

    public List<InventoryItem> getItemsGained() {
        return itemsGained;
    }

    public void setItemsGained(List<InventoryItem> itemsGained) {
        this.itemsGained = itemsGained;
    }
}
