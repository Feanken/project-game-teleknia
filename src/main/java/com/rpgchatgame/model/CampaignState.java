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
    private Map<String,Integer> reputation = new LinkedHashMap<>();
    private Set<String> codex = new LinkedHashSet<>();
    private String dialogueNode = "";
    private String trackedQuest = "";
    public Map<String,Integer> getReputation(){return reputation;}
    public void setReputation(Map<String,Integer> value){reputation=value;}
    public Set<String> getCodex(){return codex;}
    public void setCodex(Set<String> value){codex=value;}
    public String getDialogueNode(){return dialogueNode;}
    public void setDialogueNode(String value){dialogueNode=value;}
    public String getTrackedQuest(){return trackedQuest;}
    public void setTrackedQuest(String value){trackedQuest=value;}
    public boolean epilogueClosed(){return !ending.isBlank()&&!has("borderlands_reopened");}
    private String difficulty = "STANDARD";
    private String accessory = "";
    private Map<String,Integer> skillRanks = new LinkedHashMap<>();
    private Map<String,Integer> cooldowns = new LinkedHashMap<>();
    private Map<String,StatusEffect> playerEffects = new LinkedHashMap<>();
    private Map<String,StatusEffect> enemyEffects = new LinkedHashMap<>();
    public String getDifficulty(){return difficulty;}
    public void setDifficulty(String value){difficulty=value;}
    public String getAccessory(){return accessory;}
    public void setAccessory(String value){accessory=value;}
    public Map<String,Integer> getSkillRanks(){return skillRanks;}
    public void setSkillRanks(Map<String,Integer> value){skillRanks=value;}
    public Map<String,Integer> getCooldowns(){return cooldowns;}
    public void setCooldowns(Map<String,Integer> value){cooldowns=value;}
    public Map<String,StatusEffect> getPlayerEffects(){return playerEffects;}
    public void setPlayerEffects(Map<String,StatusEffect> value){playerEffects=value;}
    public Map<String,StatusEffect> getEnemyEffects(){return enemyEffects;}
    public void setEnemyEffects(Map<String,StatusEffect> value){enemyEffects=value;}

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
