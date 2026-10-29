package com.rpgchatgame.repository;

import com.rpgchatgame.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final String databaseUrl;

    public DatabaseManager() {
        try {
            Files.createDirectories(Path.of(Constants.DATABASE_DIR));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create data directory", e);
        }
        this.databaseUrl = "jdbc:sqlite:" + Path.of(Constants.DATABASE_DIR, Constants.DATABASE_FILE);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    public void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS characters (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        character_class TEXT NOT NULL,
                        level INTEGER NOT NULL,
                        exp INTEGER NOT NULL,
                        hp INTEGER NOT NULL,
                        max_hp INTEGER NOT NULL,
                        mp INTEGER NOT NULL,
                        max_mp INTEGER NOT NULL,
                        strength INTEGER NOT NULL,
                        agility INTEGER NOT NULL,
                        intelligence INTEGER NOT NULL,
                        defense INTEGER NOT NULL,
                        gold INTEGER NOT NULL,
                        current_location TEXT NOT NULL,
                        current_quest TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS inventory_items (
                        item_id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        rarity TEXT NOT NULL,
                        description TEXT NOT NULL,
                        effect TEXT NOT NULL,
                        quantity INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS story_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        speaker TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS game_state (
                        id INTEGER PRIMARY KEY,
                        current_story_summary TEXT NOT NULL,
                        current_location TEXT NOT NULL,
                        current_quest TEXT NOT NULL,
                        current_choices_json TEXT NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize database", e);
        }
    }

    public void clearAll() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM story_logs");
            statement.executeUpdate("DELETE FROM inventory_items");
            statement.executeUpdate("DELETE FROM game_state");
            statement.executeUpdate("DELETE FROM characters");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to clear save data", e);
        }
    }
}
