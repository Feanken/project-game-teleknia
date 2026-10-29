package com.rpgchatgame.repository;

import com.rpgchatgame.model.Character;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CharacterRepository {
    private final DatabaseManager databaseManager;

    public CharacterRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(Character character) {
        String sql = """
                INSERT INTO characters (id, name, character_class, level, exp, hp, max_hp, mp, max_mp, strength,
                    agility, intelligence, defense, gold, current_location, current_quest)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name,
                    character_class = excluded.character_class,
                    level = excluded.level,
                    exp = excluded.exp,
                    hp = excluded.hp,
                    max_hp = excluded.max_hp,
                    mp = excluded.mp,
                    max_mp = excluded.max_mp,
                    strength = excluded.strength,
                    agility = excluded.agility,
                    intelligence = excluded.intelligence,
                    defense = excluded.defense,
                    gold = excluded.gold,
                    current_location = excluded.current_location,
                    current_quest = excluded.current_quest
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCharacter(statement, character);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save character", e);
        }
    }

    public Optional<Character> load() {
        String sql = "SELECT * FROM characters WHERE id = 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(new Character(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("character_class"),
                    resultSet.getInt("level"),
                    resultSet.getInt("exp"),
                    resultSet.getInt("hp"),
                    resultSet.getInt("max_hp"),
                    resultSet.getInt("mp"),
                    resultSet.getInt("max_mp"),
                    resultSet.getInt("strength"),
                    resultSet.getInt("agility"),
                    resultSet.getInt("intelligence"),
                    resultSet.getInt("defense"),
                    resultSet.getInt("gold"),
                    resultSet.getString("current_location"),
                    resultSet.getString("current_quest")
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load character", e);
        }
    }

    private void bindCharacter(PreparedStatement statement, Character character) throws SQLException {
        statement.setLong(1, character.getId());
        statement.setString(2, character.getName());
        statement.setString(3, character.getCharacterClass());
        statement.setInt(4, character.getLevel());
        statement.setInt(5, character.getExp());
        statement.setInt(6, character.getHp());
        statement.setInt(7, character.getMaxHp());
        statement.setInt(8, character.getMp());
        statement.setInt(9, character.getMaxMp());
        statement.setInt(10, character.getStrength());
        statement.setInt(11, character.getAgility());
        statement.setInt(12, character.getIntelligence());
        statement.setInt(13, character.getDefense());
        statement.setInt(14, character.getGold());
        statement.setString(15, character.getCurrentLocation());
        statement.setString(16, character.getCurrentQuest());
    }
}
