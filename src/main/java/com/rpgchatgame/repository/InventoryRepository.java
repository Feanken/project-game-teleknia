package com.rpgchatgame.repository;

import com.rpgchatgame.model.InventoryItem;
import com.rpgchatgame.model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {
    private final DatabaseManager databaseManager;

    public InventoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveAll(List<InventoryItem> inventory) {
        String sql = """
                INSERT INTO inventory_items (item_id, name, type, rarity, description, effect, quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             Statement clear = connection.createStatement();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            clear.executeUpdate("DELETE FROM inventory_items");
            for (InventoryItem inventoryItem : inventory) {
                Item item = inventoryItem.getItem();
                statement.setString(1, item.getId());
                statement.setString(2, item.getName());
                statement.setString(3, item.getType().name());
                statement.setString(4, item.getRarity().name());
                statement.setString(5, item.getDescription());
                statement.setString(6, item.getEffect());
                statement.setInt(7, inventoryItem.getQuantity());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save inventory", e);
        }
    }

    public List<InventoryItem> loadAll() {
        String sql = "SELECT * FROM inventory_items ORDER BY name";
        List<InventoryItem> inventory = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Item item = new Item(
                        resultSet.getString("item_id"),
                        resultSet.getString("name"),
                        Item.ItemType.valueOf(resultSet.getString("type")),
                        Item.Rarity.valueOf(resultSet.getString("rarity")),
                        resultSet.getString("description"),
                        resultSet.getString("effect")
                );
                inventory.add(new InventoryItem(item, resultSet.getInt("quantity")));
            }
            return inventory;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load inventory", e);
        }
    }
}
