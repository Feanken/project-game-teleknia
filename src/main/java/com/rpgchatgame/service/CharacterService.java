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
            character.setHp(character.getMaxHp());
            character.setMp(character.getMaxMp());
            systemMessage.append("Level up! Kael reached level ")
                    .append(character.getLevel())
                    .append(". ");
        }
        return systemMessage.toString().trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
