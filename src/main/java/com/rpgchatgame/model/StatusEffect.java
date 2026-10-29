package com.rpgchatgame.model;

/** Remaining complete rounds and remaining potency/capacity; effects never stack. */
public class StatusEffect {
    private int turns;
    private int potency;
    public StatusEffect() {}
    public StatusEffect(int turns,int potency){this.turns=turns;this.potency=potency;}
    public int getTurns(){return turns;}
    public void setTurns(int value){turns=value;}
    public int getPotency(){return potency;}
    public void setPotency(int value){potency=value;}
}
