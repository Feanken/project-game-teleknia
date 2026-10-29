package com.rpgchatgame.repository;

import com.rpgchatgame.model.GameState;
import com.rpgchatgame.util.JsonUtil;
import com.rpgchatgame.service.SkillCatalog;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Three manual slots plus independent autosave; valid previous snapshots survive bad writes. */
public final class SaveGameRepository {
    public static final int SCHEMA = 4;
    public record SlotInfo(int slot, boolean occupied, boolean corrupt, boolean backup,
                           String name, String characterClass, int level, String location, String savedAt) {}
    public static final class SaveProblem extends IllegalStateException {
        private final int slot;
        public SaveProblem(int slot, String message, Throwable cause) { super(message, cause); this.slot=slot; }
        public int slot() { return slot; }
    }
    private record Snapshot(int version, String savedAt, String payload, String checksum) {}
    private final DatabaseManager database;
    public SaveGameRepository(DatabaseManager database) { this.database=database; }
    private static void slot(int value) { if(value<0||value>3)throw new IllegalArgumentException("Slot must be 0 (auto) or 1-3"); }
    private static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private Snapshot snapshot(GameState state) {
        validate(state);
        String payload=JsonUtil.toJson(state);
        return new Snapshot(SCHEMA,Instant.now().toString(),payload,digest(payload));
    }
    private static void validate(GameState state) {
        if(state==null||state.getCharacter()==null||state.getCampaign()==null||state.getInventory()==null||state.getStoryLogs()==null
                ||state.getCurrentChoices()==null||state.getCurrentLocation()==null||state.getCurrentQuest()==null||state.getCurrentStorySummary()==null)
            throw new IllegalStateException("Incomplete save");
        var hero=state.getCharacter();var world=state.getCampaign();
        if(hero.getName()==null||hero.getName().isBlank()||!Set.of("Fighter","Rogue","Mage").contains(hero.getCharacterClass())
                ||hero.getLevel()<1||hero.getExp()<0||hero.getGold()<0||hero.getMaxHp()<1||hero.getHp()<0||hero.getHp()>hero.getMaxHp()
                ||hero.getMaxMp()<1||hero.getMp()<0||hero.getMp()>hero.getMaxMp())throw new IllegalStateException("Invalid character data");
        if(world.getFlags()==null||world.getVisited()==null||world.getJournal()==null||world.getWeapon()==null||world.getArmor()==null
                ||world.getEnding()==null||world.getTurn()<0)throw new IllegalStateException("Invalid campaign data");
        if(world.getDifficulty()==null||!Set.of("STANDARD","STORY").contains(world.getDifficulty())||world.getAccessory()==null
                ||world.getSkillRanks()==null||world.getCooldowns()==null||world.getPlayerEffects()==null||world.getEnemyEffects()==null)
            throw new IllegalStateException("Invalid combat/build data");
        if(world.getReputation()==null||world.getCodex()==null||world.getDialogueNode()==null||world.getTrackedQuest()==null)throw new IllegalStateException("Invalid world state");
        for(var reputation:world.getReputation().entrySet())if(!Set.of("camp","keepers").contains(reputation.getKey())||reputation.getValue()==null||reputation.getValue()<0||reputation.getValue()>10)throw new IllegalStateException("Invalid reputation");
        if(!world.getDialogueNode().isBlank()&&(!com.rpgchatgame.service.WorldContent.DIALOGUES.containsKey(world.getDialogueNode())||world.getEnemy()!=null))throw new IllegalStateException("Invalid active dialogue");
        if(!world.getTrackedQuest().isBlank()&&!com.rpgchatgame.service.WorldContent.QUESTS.containsKey(world.getTrackedQuest()))throw new IllegalStateException("Invalid tracked quest");
        if(!world.getDialogueNode().isBlank()){
            var node=com.rpgchatgame.service.WorldContent.DIALOGUES.get(world.getDialogueNode());
            var npc=com.rpgchatgame.service.WorldContent.NPCS.get(node.npc());
            if(!com.rpgchatgame.service.WorldContent.location(state.getCurrentLocation()).id().equals(npc.location()))throw new IllegalStateException("Dialogue is outside its NPC location");
        }
        for(String entry:world.getCodex()){
            if(entry==null||!entry.contains(":"))throw new IllegalStateException("Invalid codex entry");
            String[] key=entry.split(":",2);boolean known=switch(key[0]){
                case "location"->com.rpgchatgame.service.WorldContent.LOCATIONS.containsKey(key[1]);
                case "npc"->com.rpgchatgame.service.WorldContent.NPCS.containsKey(key[1]);
                case "enemy"->com.rpgchatgame.service.WorldContent.MONSTERS.containsKey(key[1]);default->false;
            };
            if(!known)throw new IllegalStateException("Unknown codex entry");
        }
        int spent=0;
        for(var rank:world.getSkillRanks().entrySet()){
            if(SkillCatalog.find(state,rank.getKey())==null||rank.getValue()==null||rank.getValue()<1||rank.getValue()>3)throw new IllegalStateException("Invalid skill rank");
            spent+=rank.getValue();
        }
        if(spent>hero.getLevel())throw new IllegalStateException("Skill points exceed earned levels");
        for(var cd:world.getCooldowns().entrySet())if((!cd.getKey().equals("heal")&&SkillCatalog.find(state,cd.getKey())==null)||cd.getValue()==null||cd.getValue()<1||cd.getValue()>8)
            throw new IllegalStateException("Invalid cooldown");
        for(var effects:List.of(world.getPlayerEffects(),world.getEnemyEffects()))for(var effect:effects.entrySet()){
            if(!Set.of("burn","poison","stun","shield").contains(effect.getKey())||effect.getValue()==null||effect.getValue().getTurns()<1||effect.getValue().getTurns()>5||effect.getValue().getPotency()<0||effect.getValue().getPotency()>1000)
                throw new IllegalStateException("Invalid status effect");
        }
        if(world.getEnemy()==null&&(!world.getPlayerEffects().isEmpty()||!world.getEnemyEffects().isEmpty()||!world.getCooldowns().isEmpty()))throw new IllegalStateException("Combat effects outside an encounter");
        Set<String> ids=new HashSet<>();
        for(var item:state.getInventory()) {
            if(item==null||item.getItem()==null||item.getItem().getId()==null||item.getItem().getType()==null||item.getItem().getRarity()==null
                    ||item.getQuantity()<1||!ids.add(item.getItem().getId()))throw new IllegalStateException("Invalid inventory data");
        }
        for(var log:state.getStoryLogs())if(log==null||log.getSpeaker()==null||log.getMessage()==null||log.getCreatedAt()==null)
            throw new IllegalStateException("Invalid chat history");
        var enemy=world.getEnemy();
        if(enemy!=null&&(enemy.getId()==null||enemy.getName()==null||enemy.getMaxHp()<1||enemy.getHp()<1||enemy.getHp()>enemy.getMaxHp()))
            throw new IllegalStateException("Invalid encounter data");
    }
    private GameState decode(Snapshot data) {
        if(data.version()<1||data.version()>SCHEMA)throw new IllegalStateException("Unsupported save schema: "+data.version());
        if(!digest(data.payload()).equals(data.checksum()))throw new IllegalStateException("Save checksum mismatch");
        GameState state=JsonUtil.fromJson(data.payload(),GameState.class);
        // Sequential v1 -> v2: gameplay payload is unchanged; slots, timestamp and checksum move into the envelope.
        int version=data.version();
        while(version<SCHEMA) { if(version==1)version=2;else if(version==2)version=3;else if(version==3)version=4;else throw new IllegalStateException("Missing save migration"); }
        validate(state);return state;
    }
    private Snapshot read(Connection connection,String table,int slot) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("SELECT version,saved_at,payload,checksum FROM "+table+" WHERE slot=?")) {
            statement.setInt(1,slot);
            try(ResultSet result=statement.executeQuery()) {
                return result.next()?new Snapshot(result.getInt(1),result.getString(2),result.getString(3),result.getString(4)):null;
            }
        }
    }
    private void put(Connection connection,String table,int slot,Snapshot data) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("INSERT INTO "+table+"(slot,version,saved_at,payload,checksum) VALUES(?,?,?,?,?) ON CONFLICT(slot) DO UPDATE SET version=excluded.version,saved_at=excluded.saved_at,payload=excluded.payload,checksum=excluded.checksum")) {
            statement.setInt(1,slot);statement.setInt(2,data.version());statement.setString(3,data.savedAt());
            statement.setString(4,data.payload());statement.setString(5,data.checksum());statement.executeUpdate();
        }
    }
    public void save(GameState state) { save(0,state); }
    public void save(int slot,GameState state) {
        slot(slot);Snapshot next=snapshot(state);
        try(Connection connection=database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Snapshot previous=read(connection,"save_slots",slot);
                if(previous!=null) {
                    decode(previous); // Never promote corrupted data over a good backup.
                    put(connection,"save_backups",slot,previous);
                } else if(read(connection,"save_backups",slot)==null)put(connection,"save_backups",slot,next);
                put(connection,"save_slots",slot,next);connection.commit();
            } catch(Exception error) { connection.rollback();throw error; }
        } catch(Exception error) { throw new SaveProblem(slot,"Save failed. Existing data and backup were preserved. Recover a damaged slot before overwriting it.",error); }
    }
    public Optional<GameState> load() { return load(0); }
    public Optional<GameState> load(int slot) {
        slot(slot);
        try(Connection connection=database.getConnection()) {
            Snapshot data=read(connection,"save_slots",slot);
            return data==null?Optional.empty():Optional.of(decode(data));
        } catch(Exception error) { throw new SaveProblem(slot,"This save cannot be loaded. Open Saves to inspect or restore its backup.",error); }
    }
    public GameState restore(int slot) {
        slot(slot);
        try(Connection connection=database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Snapshot backup=read(connection,"save_backups",slot);
                if(backup==null)throw new IllegalStateException("No backup available");
                GameState restored=decode(backup);
                String payload=JsonUtil.toJson(restored);
                put(connection,"save_slots",slot,new Snapshot(SCHEMA,backup.savedAt(),payload,digest(payload)));
                connection.commit();return restored;
            } catch(Exception error) { connection.rollback();throw error; }
        } catch(Exception error) { throw new SaveProblem(slot,"Backup could not be restored. Current data was kept.",error); }
    }
    public boolean exists() { return any(); }
    public boolean any() {
        try(Connection connection=database.getConnection();Statement statement=connection.createStatement();ResultSet result=statement.executeQuery("SELECT 1 FROM save_slots UNION ALL SELECT 1 FROM save_backups LIMIT 1")) { return result.next(); }
        catch(SQLException error) { throw new IllegalStateException("Unable to inspect saves",error); }
    }
    public List<SlotInfo> slots() {
        List<SlotInfo> result=new ArrayList<>();
        try(Connection connection=database.getConnection()) {
            for(int slot=0;slot<=3;slot++) {
                Snapshot data=read(connection,"save_slots",slot);Snapshot backup=read(connection,"save_backups",slot);
                boolean recoverable=false;
                if(backup!=null)try {decode(backup);recoverable=true;}catch(RuntimeException ignored){}
                if(data==null)result.add(new SlotInfo(slot,false,false,recoverable,"","",0,"",""));
                else try {
                    var state=decode(data);var hero=state.getCharacter();
                    result.add(new SlotInfo(slot,true,false,recoverable,hero.getName(),hero.getCharacterClass(),hero.getLevel(),state.getCurrentLocation(),data.savedAt()));
                } catch(RuntimeException error) {result.add(new SlotInfo(slot,true,true,recoverable,"","",0,"",data.savedAt()));}
            }
            return result;
        } catch(SQLException error) { throw new IllegalStateException("Unable to inspect saves",error); }
    }
    /** Imported gameplay is duplicated into slot 1 and auto, with the original payload backed up. */
    public void importState(GameState state) {
        Snapshot next=snapshot(state);
        try(Connection connection=database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for(int slot:List.of(0,1)) {
                    if(read(connection,"save_slots",slot)!=null)throw new IllegalStateException("Migration refuses to overwrite existing slots");
                    put(connection,"save_backups",slot,next);put(connection,"save_slots",slot,next);
                }
                connection.commit();
            } catch(Exception error) {connection.rollback();throw error;}
        } catch(Exception error) { throw new IllegalStateException("Save import failed; legacy tables were preserved",error); }
    }
    public void importVersion11() {
        if(any())return;
        GameState legacy=null;
        try(Connection connection=database.getConnection();Statement statement=connection.createStatement();ResultSet result=statement.executeQuery("SELECT version,payload FROM campaign_save WHERE id=1")) {
            if(result.next()) {
                int version=result.getInt(1);String payload=result.getString(2);
                legacy=decode(new Snapshot(version,Instant.now().toString(),payload,digest(payload)));
            }
        } catch(Exception error) { throw new IllegalStateException("Version 1.1 save import failed. Original data was preserved.",error); }
        if(legacy!=null)importState(legacy);
    }
}
