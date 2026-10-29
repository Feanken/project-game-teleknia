package com.rpgchatgame;

import com.rpgchatgame.model.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.service.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Dependency-free integration checks. Run scripts/check-game.ps1 with JDK 21. */
public final class CampaignChecks {
    private static int assertions;
    private static final Path ROOT=Path.of("target","campaign-checks");
    private static void check(boolean value,String description) {
        assertions++;if(!value)throw new AssertionError(description);
    }
    private static DatabaseManager database() throws Exception {
        Files.createDirectories(ROOT);
        return new DatabaseManager(Files.createTempDirectory(ROOT,"case-").resolve("save.db"));
    }
    private static GameService game() throws Exception {
        GameService game=new GameService(database());game.startNewStory();return game;
    }
    private static void act(GameService game,String action) { game.performAction(action); }
    private static void goGate(GameService game) {act(game,"go_gate");act(game,"fight_wolf");}
    private static void battle(GameService game,String victoryFlag) {
        int turns=0;
        while(game.getCurrentState().getCampaign().getEnemy()!=null&&turns++<70) {
            GameState state=game.getCurrentState();var hero=state.getCharacter();
            if(hero.getHp()<hero.getMaxHp()/3&&ItemCatalog.owns(state,"small_potion"))act(game,"potion");
            else if(hero.getHp()<hero.getMaxHp()/3&&hero.getMp()>=8)act(game,"heal");
            else if((state.getCampaign().getTurn()+1)%3==0)act(game,"guard");
            else act(game,hero.getMp()>=6?"skill":"attack");
        }
        check(turns<70,"Combat terminates");
        check(game.getCurrentState().getCampaign().has(victoryFlag),"Survived encounter: "+victoryFlag);
    }
    private static GameService fullCampaign(String klass,String ending) throws Exception {
        GameService game=new GameService(database());game.startNewStory("Test "+klass,klass);
        act(game,"search");act(game,"rescue");goGate(game);battle(game,"wolf");
        act(game,"go_camp");
        check(game.buyItem("iron_sword").startsWith("Bought"),"Buy sword");
        check(game.buyItem("ranger_armor").startsWith("Bought"),"Buy armor");
        game.equipItem("iron_sword");game.equipItem("ranger_armor");
        act(game,"rest");act(game,"talk_mira");act(game,"go_trail");act(game,"herbs");
        act(game,"go_shrine");act(game,"runes");act(game,"fight_guardian");battle(game,"guardian");
        check(ItemCatalog.owns(game.getCurrentState(),"ember_blade"),"Guardian loot");game.equipItem("ember_blade");
        act(game,"sigil");act(game,"go_ruins");act(game,"archive");act(game,"locket");
        act(game,"go_camp");act(game,"return_locket");
        check(game.getCurrentState().getCampaign().has("locket_returned"),"Side quest complete");
        check(!ItemCatalog.owns(game.getCurrentState(),"silver_locket"),"Quest item turned in");
        act(game,"rest");act(game,"go_ruins");act(game,"go_tower");act(game,"fight_warden");battle(game,"warden");
        if (ending.isBlank()) return game;
        act(game,ending);
        check(!game.getCurrentState().getCampaign().getEnding().isBlank(),"Ending reached: "+ending);
        check(game.getCurrentState().getCurrentQuest().startsWith("Completed"),"Quest completed");
        check(game.getCurrentState().getCampaign().getVisited().size()==7,"Seven locations visited");
        return game;
    }
    private static void rewardsAndGates() throws Exception {
        GameService game=game();int gold=game.getCurrentState().getCharacter().getGold();
        act(game,"go_tower");check(game.getCurrentState().getCurrentLocation().equals("Burned Village"),"No teleport to boss");
        act(game,"some nonsense");check(game.getCurrentState().getCharacter().getExp()==0,"Unknown input gives no EXP");
        check(game.getCurrentState().getCharacter().getGold()==gold,"Invalid actions give no gold");
        act(game,"search");gold=game.getCurrentState().getCharacter().getGold();int exp=game.getCurrentState().getCharacter().getExp();
        act(game,"search");check(game.getCurrentState().getCharacter().getGold()==gold,"Search reward once");
        check(game.getCurrentState().getCharacter().getExp()==exp,"Search EXP once");
        goGate(game);check(game.getCurrentState().getCampaign().getEnemy()!=null,"Encounter starts");
        int hp=game.getCurrentState().getCharacter().getHp();act(game,"go_trail");
        check(game.getCurrentState().getCurrentLocation().equals("Northern Gate"),"No travel in combat");
        check(game.getCurrentState().getCharacter().getHp()==hp,"Invalid command spends no turn");
        check(game.equipItem("rusty_sword").contains("cannot"),"No midcombat equipment");
        check(game.useItem("small_potion").contains("Story"),"No free midcombat inventory heal");
        battle(game,"wolf");gold=game.getCurrentState().getCharacter().getGold();act(game,"fight_wolf");
        check(game.getCurrentState().getCampaign().getEnemy()==null,"Defeated wolf never respawns");
        check(game.getCurrentState().getCharacter().getGold()==gold,"No duplicate battle reward");
    }
    private static void economyAndRecovery() throws Exception {
        GameService game=game();
        int count=game.getCurrentState().getInventory().stream().filter(i->i.getItem().getId().equals("small_potion")).findFirst().orElseThrow().getQuantity();
        check(game.useItem("small_potion").contains("full"),"Full HP potion rejected");
        check(game.getCurrentState().getInventory().stream().filter(i->i.getItem().getId().equals("small_potion")).findFirst().orElseThrow().getQuantity()==count,"Full HP keeps item");
        check(game.buyItem("small_potion").contains("Camp"),"No remote shop");
        act(game,"rescue");act(game,"go_camp");
        check(game.buyItem("sun_sigil").contains("not for sale"),"No buying quest items");
        check(game.buyItem("iron_sword").contains("Not enough"),"Insufficient gold");
        int before=game.getCurrentState().getCharacter().getGold();
        check(game.buyItem("small_potion").startsWith("Bought"),"Successful purchase");
        check(game.getCurrentState().getCharacter().getGold()==before-12,"Purchase debits exact gold");
        act(game,"rest");game.getCurrentState().getCharacter().setGold(0);game.getCurrentState().getCharacter().setHp(1);
        act(game,"rest");check(game.getCurrentState().getCharacter().getHp()>1,"Free rough rest prevents softlock");
        check(game.getCurrentState().getCharacter().getGold()==0,"Recovery never makes negative gold");
        goGate(game);game.getCurrentState().getCharacter().setMp(0);var enemy=game.getCurrentState().getCampaign().getEnemy();int enemyHp=enemy.getHp();
        act(game,"skill");check(game.getCurrentState().getCampaign().getEnemy().getHp()==enemyHp,"No skill without MP");
        check(game.getCurrentState().getCampaign().getTurn()==0,"Failed skill spends no turn");
        game.getCurrentState().getCharacter().setGold(100);game.getCurrentState().getCharacter().setHp(1);
        act(game,"attack");
        check(game.getCurrentState().getCampaign().getEnemy()==null,"Defeat clears encounter");
        check(game.getCurrentState().getCurrentLocation().equals("Survivor Camp"),"Defeat returns to camp");
        check(game.getCurrentState().getCharacter().getGold()==80,"Defeat loses 20% gold");
        check(!game.getCurrentState().getCampaign().has("wolf"),"Defeat gives no victory");
        goGate(game);game.getCurrentState().getCharacter().setMp(0);act(game,"guard");
        check(game.getCurrentState().getCharacter().getMp()==6,"Guard restores MP");
        act(game,"flee");check(game.getCurrentState().getCurrentLocation().equals("Survivor Camp"),"Retreat to camp");
        check(!game.getCurrentState().getCampaign().has("wolf"),"Retreat gives no victory");
    }
    private static void persistenceAndAtomicity() throws Exception {
        DatabaseManager database=database();GameService game=new GameService(database);game.startNewStory("Luna","Mage");
        goGate(game);act(game,"attack");
        int enemyHp=game.getCurrentState().getCampaign().getEnemy().getHp();
        int turn=game.getCurrentState().getCampaign().getTurn();
        GameService loaded=new GameService(database);GameState restored=loaded.continueStory().orElseThrow();
        check(restored.getCampaign().getEnemy().getHp()==enemyHp,"Enemy HP persists");
        check(restored.getCampaign().getTurn()==turn,"Turn persists");
        check(restored.getCharacter().getName().equals("Luna"),"Custom name persists");
        check(restored.getStoryLogs().getFirst().getMessage().startsWith("You wake"),"Opening log saved");
        check(restored.getStoryLogs().getFirst().getCreatedAt().equals(game.getCurrentState().getStoryLogs().getFirst().getCreatedAt()),"Timestamp roundtrip");
        try(Connection connection=database.getConnection();Statement statement=connection.createStatement()) {
            statement.executeUpdate("CREATE TRIGGER fail_save BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'test failure'); END");
        }
        int logs=loaded.getCurrentState().getStoryLogs().size();
        try {act(loaded,"attack");throw new AssertionError("Save should fail");} catch(IllegalStateException expected) {assertions++;}
        check(loaded.getCurrentState().getStoryLogs().size()==logs,"Failed save does not change memory");
        check(new GameService(database).continueStory().orElseThrow().getCampaign().getEnemy().getHp()==enemyHp,"Failed save keeps disk state");
        try {loaded.startNewStory("Other","Rogue");throw new AssertionError("Replacement should fail");}catch(IllegalStateException expected){assertions++;}
        check(loaded.getCurrentState().getCharacter().getName().equals("Luna"),"Failed new game keeps previous character");
    }
    private static void legacyMigration() throws Exception {
        DatabaseManager database=database();database.initialize();
        var character=new CharacterService().createDefaultCharacter();character.setName("Legacy");character.setGold(77);
        new CharacterRepository(database).save(character);
        new InventoryRepository(database).saveAll(new InventoryService().createStarterInventory());
        var stories=new StoryLogRepository(database);stories.saveGameState("Old adventure","Northern Gate","Old quest",List.of());
        stories.append(new StoryLog(StoryLog.Speaker.PLAYER,"Old history"));
        GameState migrated=new GameService(database).continueStory().orElseThrow();
        check(migrated.getCharacter().getGold()==77,"Migration keeps gold");check(migrated.getCharacter().getName().equals("Legacy"),"Migration keeps name");
        check(migrated.getStoryLogs().getFirst().getMessage().equals("Old history"),"Migration keeps chat history");
        check(migrated.getCurrentLocation().equals("Burned Village"),"Migration restarts tracked quest");
        check(new CharacterRepository(database).load().orElseThrow().getGold()==77,"Legacy table preserved");
        check(new SaveGameRepository(database).exists(),"Migration snapshot saved");
        check(new GameService(database).continueStory().orElseThrow().getStoryLogs().size()==migrated.getStoryLogs().size(),"Migration happens once");
    }
    private static void equipmentAndCombatMath() throws Exception {
        GameService game=game();act(game,"rescue");act(game,"go_camp");
        game.getCurrentState().getCharacter().setGold(100);
        int attack=ItemCatalog.attack(game.getCurrentState()),defense=ItemCatalog.defense(game.getCurrentState());
        game.buyItem("iron_sword");game.buyItem("ranger_armor");
        check(ItemCatalog.attack(game.getCurrentState())==attack,"Buying does not auto-equip");
        game.equipItem("iron_sword");game.equipItem("ranger_armor");
        check(ItemCatalog.attack(game.getCurrentState())==attack+6,"Weapon replaces old bonus");
        check(ItemCatalog.defense(game.getCurrentState())==defense+5,"Armor bonus applied");
        check(game.buyItem("iron_sword").contains("already"),"Unique equipment cannot be bought twice");
        game.equipItem("rusty_sword");check(ItemCatalog.attack(game.getCurrentState())==attack,"Equipment switch reverses previous bonus");
        goGate(game);act(game,"attack");act(game,"attack");
        int hp=game.getCurrentState().getCharacter().getHp();
        int expected=Math.max(1,(14+8-ItemCatalog.defense(game.getCurrentState()))/2);
        act(game,"guard");check(game.getCurrentState().getCharacter().getHp()==hp-expected,"Guard halves third-turn heavy attack");
        game.getCurrentState().getCharacter().setHp(20);int mp=game.getCurrentState().getCharacter().getMp();
        act(game,"heal");check(game.getCurrentState().getCharacter().getHp()>20,"Mend heals despite counterattack");
        check(game.getCurrentState().getCharacter().getMp()==mp-8,"Mend costs MP");
        game.getCurrentState().getCharacter().setMp(0);act(game,"mana");
        check(game.getCurrentState().getCharacter().getMp()==25,"Combat mana potion restores MP");
        check(!ItemCatalog.owns(game.getCurrentState(),"mana_potion"),"Combat mana potion consumed");
    }
    private static void endingRequirements() throws Exception {
        GameService game=fullCampaign("Fighter","");
        game.getCurrentState().getCampaign().getFlags().remove("sigil");
        new InventoryService().remove(game.getCurrentState().getInventory(),"sun_sigil");
        act(game,"end_purify");check(game.getCurrentState().getCampaign().getEnding().isBlank(),"Purification requires sigil");
        act(game,"go_ruins");act(game,"go_shrine");act(game,"sigil");
        check(ItemCatalog.owns(game.getCurrentState(),"sun_sigil"),"Can retrieve missed sigil after Warden");
        act(game,"go_ruins");act(game,"go_tower");act(game,"end_purify");
        check(game.getCurrentState().getCampaign().getEnding().equals("Dawnkeeper"),"Purification unlocks after requirements");
    }
    public static void main(String[] args)throws Exception {
        rewardsAndGates();economyAndRecovery();persistenceAndAtomicity();legacyMigration();equipmentAndCombatMath();endingRequirements();
        for(String klass:List.of("Fighter","Rogue","Mage"))for(String ending:List.of("end_purify","end_seal","end_claim")) {
            GameService game=fullCampaign(klass,ending);String finished=game.getCurrentState().getCampaign().getEnding();
            int gold=game.getCurrentState().getCharacter().getGold();act(game,"end_claim");act(game,"search");
            check(game.getCurrentState().getCampaign().getEnding().equals(finished),"Ending immutable");
            check(game.getCurrentState().getCharacter().getGold()==gold,"No post-ending reward");
        }
        System.out.println("PASS / "+assertions+" assertions, 10 campaigns, all 3 classes and 3 endings, economy, combat recovery, migration, atomic-save rollback.");
    }
}
