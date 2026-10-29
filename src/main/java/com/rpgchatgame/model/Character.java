package com.rpgchatgame.model;

public class Character {
    private long id;
    private String name;
    private String characterClass;
    private int level;
    private int exp;
    private int hp;
    private int maxHp;
    private int mp;
    private int maxMp;
    private int strength;
    private int agility;
    private int intelligence;
    private int defense;
    private int gold;
    private String currentLocation;
    private String currentQuest;

    public Character() {
    }

    public Character(long id, String name, String characterClass, int level, int exp, int hp, int maxHp,
                     int mp, int maxMp, int strength, int agility, int intelligence, int defense,
                     int gold, String currentLocation, String currentQuest) {
        this.id = id;
        this.name = name;
        this.characterClass = characterClass;
        this.level = level;
        this.exp = exp;
        this.hp = hp;
        this.maxHp = maxHp;
        this.mp = mp;
        this.maxMp = maxMp;
        this.strength = strength;
        this.agility = agility;
        this.intelligence = intelligence;
        this.defense = defense;
        this.gold = gold;
        this.currentLocation = currentLocation;
        this.currentQuest = currentQuest;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public int getMaxMp() {
        return maxMp;
    }

    public void setMaxMp(int maxMp) {
        this.maxMp = maxMp;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getAgility() {
        return agility;
    }

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
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
}
