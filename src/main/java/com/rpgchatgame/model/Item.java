package com.rpgchatgame.model;

public class Item {
    public enum ItemType {
        WEAPON, ARMOR, ACCESSORY, CONSUMABLE, QUEST
    }

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }

    private String id;
    private String name;
    private ItemType type;
    private Rarity rarity;
    private String description;
    private String effect;

    public Item() {
    }

    public Item(String id, String name, ItemType type, Rarity rarity, String description, String effect) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.rarity = rarity;
        this.description = description;
        this.effect = effect;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }
}
