package com.rpgchatgame.service;

import com.rpgchatgame.model.Character;
import com.rpgchatgame.util.Constants;

public class CharacterService {
    public Character createDefaultCharacter() {
        return new Character(
                1,
                "Kael",
                "Fighter",
                1,
                0,
                100,
                100,
                30,
                30,
                10,
                8,
                6,
                8,
                20,
                "Burned Village",
                "Find the source of the attack"
        );
    }

    public Character createCharacter(String name, String klass) {
        if (name == null || name.isBlank() || name.trim().length() > 24) throw new IllegalArgumentException("Name must contain 1-24 characters");
        Character character = createDefaultCharacter();
        character.setName(name.trim());
        character.setCharacterClass(klass);
        switch (klass) {
            case "Fighter" -> { }
            case "Rogue" -> { character.setMaxHp(88); character.setAgility(15); character.setStrength(11); character.setDefense(6); }
            case "Mage" -> { character.setMaxHp(80); character.setMaxMp(55); character.setIntelligence(15); character.setStrength(7); character.setDefense(5); }
            default -> throw new IllegalArgumentException("Unknown class");
        }
        character.setHp(character.getMaxHp()); character.setMp(character.getMaxMp());
        return character;
    }

    public String applyProgress(Character character, int hpChange, int mpChange, int expGain, int goldChange) {
        character.setHp(clamp(character.getHp() + hpChange, 0, character.getMaxHp()));
        character.setMp(clamp(character.getMp() + mpChange, 0, character.getMaxMp()));
        character.setGold(Math.max(0, character.getGold() + goldChange));
        character.setExp(character.getExp() + expGain);

        StringBuilder systemMessage = new StringBuilder();
        while (character.getExp() >= Constants.LEVEL_UP_EXP) {
            character.setExp(character.getExp() - Constants.LEVEL_UP_EXP);
            character.setLevel(character.getLevel() + 1);
            character.setMaxHp(character.getMaxHp() + 12);
            character.setMaxMp(character.getMaxMp() + 5);
            character.setStrength(character.getStrength() + 2);
            character.setDefense(character.getDefense() + 1);
            character.setAgility(character.getAgility() + 1);
            character.setIntelligence(character.getIntelligence() + 2);
            character.setHp(character.getMaxHp());
            character.setMp(character.getMaxMp());
            systemMessage.append("Level up! ").append(character.getName()).append(" reached level ")
                    .append(character.getLevel())
                    .append(". ");
        }
        return systemMessage.toString().trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
