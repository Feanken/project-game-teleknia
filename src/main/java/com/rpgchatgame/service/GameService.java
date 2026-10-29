package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import com.rpgchatgame.repository.*;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;

public class GameService {
    private final DatabaseManager database;
    private final SaveGameRepository saves;
    private final CharacterService characters=new CharacterService();
    private final InventoryService inventory=new InventoryService();
    private final LocalStoryGeneratorService story=new LocalStoryGeneratorService();
    private GameState currentState;

    public GameService() { this(new DatabaseManager()); }
    public GameService(DatabaseManager database) {
        this.database=database;database.initialize();saves=new SaveGameRepository(database);
    }
    public GameState startNewStory() { return startNewStory("Kael","Fighter"); }
    public GameState startNewStory(String name,String klass) {
        GameState state=new GameState();
        state.setCharacter(characters.createCharacter(name,klass));
        state.setInventory(inventory.createStarterInventory());
        state.setCurrentLocation("Burned Village");state.getCampaign().getVisited().add("Burned Village");
        state.getCampaign().getJournal().add("A village in ashes. Rescue its people and discover why the northern road burns.");
        String opening="You wake in the ashes of your village. A strange roar echoes from the northern gate. Beneath a fallen beam, a woman named Mira calls for help. This time, your choices will leave a mark.";
        state.setCurrentStorySummary(opening);state.setCurrentQuest(story.quest(state));
        state.getCharacter().setCurrentQuest(state.getCurrentQuest());
        state.setCurrentChoices(story.choices(state));
        state.getStoryLogs().add(new StoryLog(StoryLog.Speaker.GAME_MASTER,opening));
        commit(state);return currentState;
    }
    public Optional<GameState> continueStory() {
        Optional<GameState> saved=saves.load();
        if(saved.isPresent()) {
            currentState=saved.get();currentState.setCurrentChoices(story.choices(currentState));return saved;
        }
        // Legacy tables are read once and retained as a fallback/backup, never cleared.
        var legacy=new CharacterRepository(database).load();
        var oldStory=new StoryLogRepository(database).loadGameState();
        if(legacy.isEmpty()||oldStory.isEmpty())return Optional.empty();
        GameState migrated=new GameState();migrated.setCharacter(legacy.get());
        migrated.setInventory(new InventoryRepository(database).loadAll());
        for(var item:migrated.getInventory()) {
            var entry=ItemCatalog.entry(item.getItem().getId());
            if(entry!=null)item.setItem(ItemCatalog.stack(item.getItem().getId(),1).getItem());
        }
        migrated.setStoryLogs(new StoryLogRepository(database).loadAll());
        migrated.setCurrentLocation("Burned Village");migrated.getCharacter().setCurrentLocation("Burned Village");
        migrated.getCampaign().getVisited().add("Burned Village");
        String message="Campaign expanded: your character, inventory and history were kept. You return to the village to begin the new quest line; the old story had no tracked quest milestones.";
        migrated.getStoryLogs().add(new StoryLog(StoryLog.Speaker.SYSTEM,message));
        migrated.getCampaign().getJournal().add(message);migrated.setCurrentStorySummary(message);
        migrated.setCurrentQuest(story.quest(migrated));migrated.getCharacter().setCurrentQuest(migrated.getCurrentQuest());
        migrated.setCurrentChoices(story.choices(migrated));commit(migrated);return Optional.of(currentState);
    }
    public GameState getCurrentState() {
        if(currentState==null)currentState=continueStory().orElseGet(this::startNewStory);
        return currentState;
    }
    public boolean hasSavedGame() { return saves.exists()||new CharacterRepository(database).load().isPresent(); }
    private GameState copy() { return JsonUtil.fromJson(JsonUtil.toJson(getCurrentState()),GameState.class); }
    private void commit(GameState next) { saves.save(next);currentState=next; }

    public StoryResponse performAction(String action) {
        if(action==null||action.isBlank()||action.length()>250)throw new IllegalArgumentException("Enter an action of 1-250 characters");
        GameState next=copy();
        String label=next.getCurrentChoices().stream().filter(c->c.getAction().equals(action)).map(Choice::getLabel).findFirst().orElse(action);
        next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.PLAYER,label));
        StoryResponse response=story.generateNextScene(next,action);
        String levelMessage=characters.applyProgress(next.getCharacter(),response.getHpChange(),response.getMpChange(),response.getExpGain(),response.getGoldChange());
        for(var gained:response.getItemsGained())inventory.addItem(next.getInventory(),gained);
        if(response.getNewLocation()!=null)next.setCurrentLocation(response.getNewLocation());
        next.getCharacter().setCurrentLocation(next.getCurrentLocation());
        next.setCurrentQuest(story.quest(next));next.getCharacter().setCurrentQuest(next.getCurrentQuest());
        next.setCurrentChoices(story.choices(next));
        next.setCurrentStorySummary(response.getNarration());
        next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.GAME_MASTER,response.getNarration()));
        String result=changes(response)+(levelMessage.isBlank()?"":" "+levelMessage);
        if(!result.isBlank())next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.SYSTEM,result.trim()));
        commit(next);return response;
    }
    private String changes(StoryResponse response) {
        List<String> changes=new ArrayList<>();
        if(response.getHpChange()!=0)changes.add("HP "+signed(response.getHpChange()));
        if(response.getMpChange()!=0)changes.add("MP "+signed(response.getMpChange()));
        if(response.getExpGain()!=0)changes.add("EXP "+signed(response.getExpGain()));
        if(response.getGoldChange()!=0)changes.add("Gold "+signed(response.getGoldChange()));
        for(var item:response.getItemsGained())changes.add(item.getItem().getName()+" x"+item.getQuantity());
        return String.join(" / ",changes);
    }
    private String signed(int value) { return value>0?"+"+value:Integer.toString(value); }
    private String record(GameState next,String message) {
        next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.SYSTEM,message));commit(next);return message;
    }
    public String useItem(String id) {
        GameState next=copy();
        if(next.getCampaign().getEnemy()!=null)return "Use combat potions from Story; drinking one spends your turn.";
        if(!next.getCampaign().getEnding().isBlank())return "Your journey is complete. Start a new story to play again.";
        return record(next,inventory.useItem(next.getInventory(),next.getCharacter(),id));
    }
    public String equipItem(String id) {
        GameState next=copy();var entry=ItemCatalog.entry(id);
        if(next.getCampaign().getEnemy()!=null)return "Equipment cannot be changed during combat.";
        if(!next.getCampaign().getEnding().isBlank())return "Your journey is complete.";
        if(entry==null||!ItemCatalog.owns(next,id))return "Item not found.";
        if(entry.item().getType()==Item.ItemType.WEAPON)next.getCampaign().setWeapon(id);
        else if(entry.item().getType()==Item.ItemType.ARMOR)next.getCampaign().setArmor(id);
        else return "This item cannot be equipped.";
        return record(next,"Equipped "+entry.item().getName()+". Attack "+ItemCatalog.attack(next)+", Defense "+ItemCatalog.defense(next)+".");
    }
    public boolean canShop() { var state=getCurrentState();return state.getCurrentLocation().equals("Survivor Camp")&&state.getCampaign().getEnemy()==null&&state.getCampaign().getEnding().isBlank(); }
    public String buyItem(String id) {
        if(!canShop())return "The market is at Survivor Camp. Finish combat before trading.";
        GameState next=copy();var entry=ItemCatalog.entry(id);
        if(entry==null||!ItemCatalog.shop().contains(entry))return "This item is not for sale.";
        if(entry.item().getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(next,id))return "You already own this equipment.";
        if(next.getCharacter().getGold()<entry.price())return "Not enough gold.";
        next.getCharacter().setGold(next.getCharacter().getGold()-entry.price());
        inventory.addItem(next.getInventory(),ItemCatalog.stack(id,1));
        return record(next,"Bought "+entry.item().getName()+" for "+entry.price()+" gold.");
    }
}
