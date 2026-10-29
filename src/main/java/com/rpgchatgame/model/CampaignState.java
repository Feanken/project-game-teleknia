package com.rpgchatgame.model;

import java.util.*;

/** Persisted world progression; flags make one-time rewards and quest gates explicit. */
public class CampaignState {
    private Set<String> flags = new LinkedHashSet<>();
    private Set<String> visited = new LinkedHashSet<>();
    private List<String> journal = new ArrayList<>();
    private Enemy enemy;
    private int turn;
    private String weapon = "rusty_sword";
    private String armor = "";
    private String ending = "";
    public Set<String> getFlags() { return flags; }
    public void setFlags(Set<String> value) { flags = value; }
    public Set<String> getVisited() { return visited; }
    public void setVisited(Set<String> value) { visited = value; }
    public List<String> getJournal() { return journal; }
    public void setJournal(List<String> value) { journal = value; }
    public Enemy getEnemy() { return enemy; }
    public void setEnemy(Enemy value) { enemy = value; }
    public int getTurn() { return turn; }
    public void setTurn(int value) { turn = value; }
    public String getWeapon() { return weapon; }
    public void setWeapon(String value) { weapon = value; }
    public String getArmor() { return armor; }
    public void setArmor(String value) { armor = value; }
    public String getEnding() { return ending; }
    public void setEnding(String value) { ending = value; }
    public boolean has(String flag) { return flags.contains(flag); }
    public boolean mark(String flag, String entry) {
        if (!flags.add(flag)) return false;
        if (entry != null && !entry.isBlank()) journal.add(entry);
        return true;
    }
    public String chapter() {
        if (!ending.isBlank()) return "EPILOGUE";
        if (has("guardian")) return "III / THE CINDER CROWN";
        if (has("wolf")) return "II / WHISPERS IN EMBERWOOD";
        return "I / ASHES OF THE VILLAGE";
    }
}
