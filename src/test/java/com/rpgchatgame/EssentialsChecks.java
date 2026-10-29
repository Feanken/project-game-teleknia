package com.rpgchatgame;

import com.rpgchatgame.model.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.util.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Headless 1.2 integration checks. No JavaFX toolkit, window, or audio device required. */
public final class EssentialsChecks {
    private static int assertions;
    private static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    private static void fails(Runnable work,String label){try{work.run();throw new AssertionError(label);}catch(IllegalStateException|IllegalArgumentException expected){assertions++;}}
    private static DatabaseManager database()throws Exception{Path root=Path.of("target","essentials-checks");Files.createDirectories(root);return new DatabaseManager(Files.createTempDirectory(root,"case-").resolve("save.db"));}
    private static void sql(DatabaseManager database,String sql)throws Exception{try(var c=database.getConnection();var s=c.createStatement()){s.executeUpdate(sql);}}
    private static String value(DatabaseManager database,String sql)throws Exception{try(var c=database.getConnection();var s=c.createStatement();var r=s.executeQuery(sql)){return r.next()?r.getString(1):null;}}
    private static void slots()throws Exception{
        var db=database();var game=new GameService(db);
        fails(()->game.saveSlot(1),"Saving must not invent a character");
        game.startNewStory("Aria","Mage");game.performAction("search");game.saveSlot(1);
        game.startNewStory("Bram","Fighter");game.saveSlot(2);game.startNewStory("Cora","Rogue");game.saveSlot(3);
        var repository=new SaveGameRepository(db);
        check(repository.load(1).orElseThrow().getCharacter().getName().equals("Aria"),"Slot 1 isolated");
        check(repository.load(2).orElseThrow().getCharacter().getName().equals("Bram"),"Slot 2 isolated");
        check(repository.load(3).orElseThrow().getCharacter().getName().equals("Cora"),"Slot 3 isolated");
        check(repository.load().orElseThrow().getCharacter().getName().equals("Cora"),"Autosave separate");
        game.loadSlot(1);check(game.getCurrentState().getCharacter().getName().equals("Aria"),"Load manual sets current game");
        check(repository.load().orElseThrow().getCharacter().getName().equals("Aria"),"Load manual updates auto");
        game.performAction("rescue");check(!repository.load(1).orElseThrow().getCampaign().has("survivors"),"Actions leave manual checkpoint unchanged");
        check(repository.load().orElseThrow().getCampaign().has("survivors"),"Actions autosave");
        for(var info:game.saveSlots()){
            check(info.occupied()&&!info.corrupt()&&info.backup(),"Slot metadata and first-save backup");
            check(!info.savedAt().isBlank()&&!info.name().isBlank()&&!info.location().isBlank()&&info.level()>0,"Metadata complete");
        }
        fails(()->game.saveSlot(0),"Autosave not a manual target");fails(()->repository.load(4),"Reject nonexistent slot");
    }
    private static void backupAndDamage()throws Exception{
        var db=database();var game=new GameService(db);game.startNewStory("Before","Mage");game.saveSlot(1);
        game.performAction("search");game.saveSlot(1);
        String backup=value(db,"SELECT payload FROM save_backups WHERE slot=1");
        sql(db,"UPDATE save_slots SET payload='broken' WHERE slot=1");
        var repository=new SaveGameRepository(db);
        check(repository.slots().get(1).corrupt()&&repository.slots().get(1).backup(),"Corrupt slot shown with recovery");
        fails(()->game.loadSlot(1),"Corruption not loaded as default character");
        fails(()->game.saveSlot(1),"Cannot overwrite corrupted save");
        check(value(db,"SELECT payload FROM save_backups WHERE slot=1").equals(backup),"Good backup not replaced by corrupt primary");
        game.restoreBackup(1);check(!game.getCurrentState().getCampaign().has("supplies"),"Restore previous manual checkpoint");
        check(!repository.slots().get(1).corrupt(),"Restored primary valid");
        game.performAction("search");
        String validAutoBackup=value(db,"SELECT payload FROM save_backups WHERE slot=0");
        sql(db,"UPDATE save_slots SET checksum='bad' WHERE slot=0");
        fails(()->game.performAction("rescue"),"Broken autosave blocks commit");
        check(!game.getCurrentState().getCampaign().has("survivors"),"Failed autosave preserves memory");
        game.restoreBackup(0);check(JsonUtil.toJson(game.getCurrentState()).equals(validAutoBackup),"Restore autosave exact state");
        sql(db,"UPDATE save_slots SET payload='broken' WHERE slot=2"); // absent slot: no accidental insertion
        check(repository.load(2).isEmpty(),"Empty slots remain empty");
        sql(db,"UPDATE save_backups SET payload='broken' WHERE slot=1");
        check(!repository.slots().get(1).backup(),"Corrupted backup not offered");
        fails(()->game.restoreBackup(1),"Corrupt backup rejected");
        check(!repository.slots().get(1).corrupt(),"Rejected recovery leaves valid primary");
    }
    private static void missingAutosave()throws Exception{
        var db=database();var game=new GameService(db);game.startNewStory("RecoverMe","Mage");
        sql(db,"DELETE FROM save_slots WHERE slot=0");
        var reopened=new GameService(db);
        check(reopened.hasSavedGame(),"Orphan backup is discoverable");
        check(reopened.continueStory().isEmpty(),"Missing primary remains missing");
        fails(()->reopened.getCurrentState(),"Missing autosave does not silently invent a character");
        check(value(db,"SELECT COUNT(*) FROM save_slots").equals("0"),"Failed implicit load does not overwrite orphan backup");
        reopened.restoreBackup(0);check(reopened.getCurrentState().getCharacter().getName().equals("RecoverMe"),"Orphan backup restores character");
    }
    private static void combatAndRollback()throws Exception{
        var db=database();var game=new GameService(db);game.startNewStory();game.performAction("go_gate");game.performAction("fight_wolf");game.performAction("attack");game.saveSlot(2);
        int hp=game.getCurrentState().getCampaign().getEnemy().getHp(),turn=game.getCurrentState().getCampaign().getTurn();
        game.performAction("guard");game.loadSlot(2);
        check(game.getCurrentState().getCampaign().getEnemy().getHp()==hp&&game.getCurrentState().getCampaign().getTurn()==turn,"Manual combat save/load exact");
        String auto=value(db,"SELECT payload FROM save_slots WHERE slot=0"),backup=value(db,"SELECT payload FROM save_backups WHERE slot=0");
        sql(db,"CREATE TRIGGER block_auto BEFORE UPDATE ON save_slots WHEN NEW.slot=0 BEGIN SELECT RAISE(ABORT,'failed commit'); END");
        fails(()->game.performAction("attack"),"Injected commit failure");
        check(value(db,"SELECT payload FROM save_slots WHERE slot=0").equals(auto),"Primary rolled back");
        check(value(db,"SELECT payload FROM save_backups WHERE slot=0").equals(backup),"Backup rolled back");
        check(game.getCurrentState().getCampaign().getEnemy().getHp()==hp,"Memory rolled back");
        fails(()->game.loadSlot(2),"Load cannot replace memory when autosave fails");
    }
    private static void migration()throws Exception{
        var source=new GameService(database());source.startNewStory("Version11","Rogue");source.performAction("go_gate");source.performAction("fight_wolf");source.performAction("attack");
        String original=JsonUtil.toJson(source.getCurrentState());var db=database();db.initialize();
        try(var c=db.getConnection();var s=c.prepareStatement("INSERT INTO campaign_save(id,version,payload) VALUES(1,1,?)")){s.setString(1,original);s.executeUpdate();}
        var imported=new GameService(db);var repo=new SaveGameRepository(db);
        check(JsonUtil.toJson(repo.load(1).orElseThrow()).equals(original),"1.1 payload migrated exactly into manual slot 1");
        check(JsonUtil.toJson(repo.load().orElseThrow()).equals(original),"1.1 payload migrated into autosave");
        check(value(db,"SELECT payload FROM campaign_save WHERE id=1").equals(original),"Original 1.1 retained before migration");
        check(value(db,"SELECT version FROM save_slots WHERE slot=1").equals("2"),"Sequential envelope schema upgraded");
        imported.loadSlot(0);imported.performAction("guard");String changed=JsonUtil.toJson(imported.getCurrentState());
        check(JsonUtil.toJson(new GameService(db).continueStory().orElseThrow()).equals(changed),"Import never repeats over new progress");
        sql(db,"UPDATE save_slots SET version=99 WHERE slot=1");
        fails(()->repo.load(1),"Future schema protected");fails(()->repo.save(1,source.getCurrentState()),"Future schema not overwritten");
        check(repo.slots().get(1).backup(),"Supported backup remains available for future schema slot");
        var failed=database();failed.initialize();
        try(var c=failed.getConnection();var s=c.prepareStatement("INSERT INTO campaign_save(id,version,payload) VALUES(1,1,?)")){s.setString(1,original);s.executeUpdate();}
        sql(failed,"CREATE TRIGGER fail_migration BEFORE INSERT ON save_slots WHEN NEW.slot=1 BEGIN SELECT RAISE(ABORT,'migration failure'); END");
        fails(()->new GameService(failed),"Interrupted migration fails explicitly");
        check(value(failed,"SELECT COUNT(*) FROM save_slots").equals("0")&&value(failed,"SELECT COUNT(*) FROM save_backups").equals("0"),"Interrupted import rolls back all rows");
        check(value(failed,"SELECT payload FROM campaign_save WHERE id=1").equals(original),"Failed import preserves original save");
    }
    private static void preferencesAndText()throws Exception{
        Path root=Path.of("target","essentials-checks");Files.createDirectories(root);Path file=Files.createTempDirectory(root,"prefs-").resolve("ui.properties");
        UiSettings settings=new UiSettings(file);check(settings.language().equals("en")&&!settings.tutorialSeen(),"Default preferences");
        settings.setLanguage("id");settings.setTextScale(1.2);settings.setFullscreen(true);settings.setTutorialSeen(true);settings.setWindowSize(1000,720);
        settings=new UiSettings(file);check(settings.language().equals("id")&&settings.textScale()==1.2&&settings.fullscreen()&&settings.tutorialSeen(),"Preferences persist");
        check(settings.width()==1000&&settings.height()==720,"Window size persists");settings.setTextScale(90);check(settings.textScale()==1.2,"Text size clamped");
        settings.setTextScale(Double.NaN);check(settings.textScale()==1.2,"Ignore invalid size");
        Files.writeString(file,"language=bad\ntextScale=NaN\nwidth=0\nheight=oops\n");settings=new UiSettings(file);
        check(settings.language().equals("en")&&settings.textScale()==1&&settings.width()==980&&settings.height()==820,"Bad preferences recover to bounded defaults");
        check(UiText.resourcesMatch(),"Both UI languages have identical keys");UiText.setLanguage("id");
        check(UiText.tr("Inventory").equals("Inventori"),"Indonesian label");
        check(UiText.tr("Manual slot 2").equals("Slot manual 2"),"Dynamic UI translation");
        check(UiText.format("Step {0} of {1}",3,5).equals("Langkah 3 dari 5"),"Tutorial format translation");
        check(UiText.tr("A whole story sentence.").equals("A whole story sentence."),"Narration remains untouched");
        UiText.setLanguage("en");check(UiText.tr("Inventory").equals("Inventory"),"Switch back to English");
    }
    private static void inventory()throws Exception{
        var game=new GameService(database());game.startNewStory();var state=game.getCurrentState();
        int count=state.getInventory().size();var all=InventoryQuery.select(state.getInventory(),null,InventoryQuery.Sort.NAME);
        check(all.size()==count,"All items filter");
        var consumables=InventoryQuery.select(state.getInventory(),Item.ItemType.CONSUMABLE,InventoryQuery.Sort.QUANTITY);
        check(consumables.size()==2&&consumables.getFirst().getItem().getId().equals("small_potion"),"Type and quantity ordering");
        check(InventoryQuery.select(state.getInventory(),Item.ItemType.ARMOR,InventoryQuery.Sort.NAME).isEmpty(),"Empty filter");
        check(state.getInventory().getFirst().getItem().getId().equals("rusty_sword"),"Sorting does not mutate inventory");
        check(InventoryQuery.comparison(state,"iron_sword").equals("Attack: 12 -> 18 (+6)"),"Weapon comparison replaces bonus");
        check(InventoryQuery.comparison(state,"ranger_armor").equals("Defense: 8 -> 13 (+5)"),"Armor comparison");
        check(state.getCampaign().getWeapon().equals("rusty_sword"),"Preview never equips");
        check(ItemCatalog.shop().size()==4&&ItemCatalog.entry("small_potion").hp()==35,"External item definitions retain rules");
        check(ActionDefinitions.label("attack","fallback").equals("Attack"),"External action label available");
    }
    public static void main(String[] args)throws Exception{
        slots();backupAndDamage();missingAutosave();combatAndRollback();migration();preferencesAndText();inventory();
        System.out.println("PASS / "+assertions+" essentials checks: isolated slots, metadata, backups, corruption, migration, rollback, preferences, localization, inventory.");
    }
}
