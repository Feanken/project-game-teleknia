package com.rpgchatgame.repository;

import com.rpgchatgame.model.GameState;
import com.rpgchatgame.util.JsonUtil;
import java.sql.*;
import java.util.Optional;

/** One atomic snapshot contains character, world, combat, inventory AND opening/history logs. */
public final class SaveGameRepository {
    private final DatabaseManager database;
    public SaveGameRepository(DatabaseManager database) { this.database=database; }
    public void save(GameState state) {
        String payload=JsonUtil.toJson(state);
        try (Connection connection=database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement=connection.prepareStatement("INSERT INTO campaign_save(id,version,payload) VALUES(1,1,?) ON CONFLICT(id) DO UPDATE SET version=1,payload=excluded.payload")) {
                statement.setString(1,payload);statement.executeUpdate();connection.commit();
            } catch(Exception error) { connection.rollback();throw error; }
        } catch(SQLException error) { throw new IllegalStateException("Unable to save campaign. Previous save was preserved.",error); }
    }
    public Optional<GameState> load() {
        try(Connection connection=database.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT version,payload FROM campaign_save WHERE id=1");ResultSet result=statement.executeQuery()) {
            if(!result.next())return Optional.empty();
            if(result.getInt("version")!=1)throw new IllegalStateException("Unsupported campaign save version");
            GameState state=JsonUtil.fromJson(result.getString("payload"),GameState.class);
            if(state==null||state.getCharacter()==null||state.getCampaign()==null||state.getInventory()==null||state.getStoryLogs()==null)
                throw new IllegalStateException("Incomplete campaign save; refusing to overwrite it");
            return Optional.of(state);
        } catch(SQLException error) { throw new IllegalStateException("Unable to load campaign",error); }
    }
    public boolean exists() {
        try(Connection connection=database.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT 1 FROM campaign_save WHERE id=1");ResultSet result=statement.executeQuery()) { return result.next(); }
        catch(SQLException error) { throw new IllegalStateException("Unable to check campaign save",error); }
    }
}
