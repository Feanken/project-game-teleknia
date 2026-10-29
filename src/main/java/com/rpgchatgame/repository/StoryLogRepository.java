package com.rpgchatgame.repository;

import com.rpgchatgame.model.Choice;
import com.rpgchatgame.model.StoryLog;
import com.rpgchatgame.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class StoryLogRepository {
    private final DatabaseManager databaseManager;

    public StoryLogRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void append(StoryLog storyLog) {
        String sql = "INSERT INTO story_logs (speaker, message, created_at) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, storyLog.getSpeaker().name());
            statement.setString(2, storyLog.getMessage());
            statement.setString(3, storyLog.getCreatedAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to append story log", e);
        }
    }

    public void saveGameState(String summary, String location, String quest, List<Choice> choices) {
        String sql = """
                INSERT INTO game_state (id, current_story_summary, current_location, current_quest, current_choices_json)
                VALUES (1, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    current_story_summary = excluded.current_story_summary,
                    current_location = excluded.current_location,
                    current_quest = excluded.current_quest,
                    current_choices_json = excluded.current_choices_json
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, summary);
            statement.setString(2, location);
            statement.setString(3, quest);
            statement.setString(4, JsonUtil.toJson(choices));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save game state", e);
        }
    }

    public List<StoryLog> loadAll() {
        String sql = "SELECT * FROM story_logs ORDER BY id";
        List<StoryLog> storyLogs = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                storyLogs.add(new StoryLog(
                        resultSet.getLong("id"),
                        StoryLog.Speaker.valueOf(resultSet.getString("speaker")),
                        resultSet.getString("message"),
                        LocalDateTime.parse(resultSet.getString("created_at"))
                ));
            }
            return storyLogs;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load story logs", e);
        }
    }

    public Optional<SavedStoryState> loadGameState() {
        String sql = "SELECT * FROM game_state WHERE id = 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            Choice[] choices = JsonUtil.fromJson(resultSet.getString("current_choices_json"), Choice[].class);
            return Optional.of(new SavedStoryState(
                    resultSet.getString("current_story_summary"),
                    resultSet.getString("current_location"),
                    resultSet.getString("current_quest"),
                    new ArrayList<>(Arrays.asList(choices))
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load game state", e);
        }
    }

    public void clearLogs() {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM story_logs");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to clear story logs", e);
        }
    }

    public record SavedStoryState(String summary, String location, String quest, List<Choice> choices) {
    }
}
