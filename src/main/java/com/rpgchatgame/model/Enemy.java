package com.rpgchatgame.model;

public class Enemy {
    private String id;
    private String name;
    private int hp;
    private int maxHp;
    private int attack;
    private int defense;
    public Enemy() {}
    public Enemy(String id, String name, int hp, int attack, int defense) {
        this.id=id; this.name=name; this.hp=hp; this.maxHp=hp; this.attack=attack; this.defense=defense;
    }
    public String getId() { return id; }
    public void setId(String value) { id=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public int getHp() { return hp; }
    public void setHp(int value) { hp=value; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int value) { maxHp=value; }
    public int getAttack() { return attack; }
    public void setAttack(int value) { attack=value; }
    public int getDefense() { return defense; }
    public void setDefense(int value) { defense=value; }
}
