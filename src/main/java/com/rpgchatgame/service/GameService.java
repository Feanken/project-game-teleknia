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
        saves.importVersion11();
        if(!saves.any()) migrateLegacy();
    }
    public GameState startNewStory() { return startNewStory("Kael","Fighter"); }
    public GameState startNewStory(String name,String klass) { return startNewStory(name,klass,"STANDARD"); }
    public GameState startNewStory(String name,String klass,String difficulty) {
        if(!Set.of("STANDARD","STORY").contains(difficulty))throw new IllegalArgumentException("Unknown difficulty");
        GameState state=new GameState();
        state.setCharacter(characters.createCharacter(name,klass));
        state.getCampaign().setDifficulty(difficulty);
        state.setInventory(inventory.createStarterInventory());
        state.setCurrentLocation("Burned Village");state.getCampaign().getVisited().add("Burned Village");
        state.getCampaign().getJournal().add("A village in ashes. Rescue its people and discover why the northern road burns.");
        String opening="You wake in the ashes of your village. A strange roar echoes from the northern gate. Beneath a fallen beam, a woman named Mira calls for help. This time, your choices will leave a mark.";
        state.setCurrentStorySummary(opening);state.setCurrentQuest(story.quest(state));
        state.getCharacter().setCurrentQuest(state.getCurrentQuest());
        WorldService.prepare(state);
        state.setCurrentChoices(story.choices(state));
        state.getStoryLogs().add(new StoryLog(StoryLog.Speaker.GAME_MASTER,opening));
        commit(state);return currentState;
    }
    public Optional<GameState> continueStory() { return loadSlot(0); }
    public Optional<GameState> loadSlot(int slot) {
        Optional<GameState> saved=saves.load(slot);
        if(saved.isPresent()) {
            GameState next=saved.get();WorldService.prepare(next);BondService.prepare(next);next.setCurrentChoices(story.choices(next));
            if(slot!=0)saves.save(next);
            currentState=next;
        }
        return saved;
    }
    public void saveSlot(int slot) {
        if(slot<1||slot>3)throw new IllegalArgumentException("Choose manual slot 1-3");
        if(currentState==null)throw new IllegalStateException("Start or load a character before making a manual save.");
        saves.save(slot,currentState);
    }
    public java.util.List<SaveGameRepository.SlotInfo> saveSlots() { return saves.slots(); }
    public void restoreBackup(int slot) {
        GameState next=saves.restore(slot);WorldService.prepare(next);BondService.prepare(next);next.setCurrentChoices(story.choices(next));
        if(slot!=0)saves.save(next);
        currentState=next;
    }
    public boolean hasCurrentGame() { return currentState!=null; }
    private void migrateLegacy() {
        // Legacy tables are read once and retained as a fallback/backup, never cleared.
        var legacy=new CharacterRepository(database).load();
        var oldStory=new StoryLogRepository(database).loadGameState();
        if(legacy.isEmpty()||oldStory.isEmpty())return;
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
        WorldService.prepare(migrated);migrated.setCurrentChoices(story.choices(migrated));saves.importState(migrated);
    }
    public GameState getCurrentState() {
        if(currentState==null) {
            var saved=continueStory();
            if(saved.isPresent())currentState=saved.get();
            else if(saves.any())throw new SaveGameRepository.SaveProblem(0,"Autosave is missing. Open Saves to load a manual checkpoint or recover its backup.",null);
            else currentState=startNewStory();
        }
        return currentState;
    }
    public boolean hasSavedGame() { return saves.any(); }
    private GameState copy() { return JsonUtil.fromJson(JsonUtil.toJson(getCurrentState()),GameState.class); }
    private void commit(GameState next) { BondService.prepare(next);saves.save(next);currentState=next; }

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
        WorldService.prepare(next);
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
        if(next.getCampaign().epilogueClosed())return "Your journey is complete. Start a new story to play again.";
        return record(next,inventory.useItem(next.getInventory(),next.getCharacter(),id));
    }
    public String equipItem(String id) {
        GameState next=copy();var entry=ItemCatalog.entry(id);
        if(next.getCampaign().getEnemy()!=null)return "Equipment cannot be changed during combat.";
        if(next.getCampaign().epilogueClosed())return "Your journey is complete.";
        if(entry==null||!ItemCatalog.owns(next,id))return "Item not found.";
        if(entry.item().getType()==Item.ItemType.WEAPON)next.getCampaign().setWeapon(id);
        else if(entry.item().getType()==Item.ItemType.ARMOR)next.getCampaign().setArmor(id);
        else if(entry.item().getType()==Item.ItemType.ACCESSORY)next.getCampaign().setAccessory(id);
        else return "This item cannot be equipped.";
        return record(next,"Equipped "+entry.item().getName()+". Attack "+ItemCatalog.attack(next)+", Defense "+ItemCatalog.defense(next)+".");
    }
    public void trackQuest(String id) {
        if(!WorldContent.QUESTS.containsKey(id))throw new IllegalArgumentException("Unknown quest");
        GameState next=copy();next.getCampaign().setTrackedQuest(id);commit(next);
    }
    public String upgradeSkill(String id) {
        GameState next=copy();
        if(next.getCampaign().getEnemy()!=null||next.getCampaign().epilogueClosed())return "Change your build outside combat before the ending.";
        if(SkillCatalog.find(next,id)==null)return "This skill belongs to another class.";
        int rank=SkillCatalog.rank(next,id);
        if(rank>=3)return "Maximum rank reached.";
        if(SkillCatalog.points(next)<1)return "No skill points available.";
        next.getCampaign().getSkillRanks().put(id,rank+1);
        return record(next,"Upgraded "+SkillCatalog.find(next,id).name()+" to rank "+(rank+1)+".");
    }
    public String resetBuild() {
        if(!canShop())return "Reset your build at Survivor Camp outside combat.";
        GameState next=copy();
        if(next.getCampaign().getSkillRanks().isEmpty())return "No allocated points to reset.";
        if(next.getCharacter().getGold()<20)return "Not enough gold.";
        next.getCharacter().setGold(next.getCharacter().getGold()-20);next.getCampaign().getSkillRanks().clear();
        return record(next,"Build reset for 20 gold. All allocated skill points were refunded; HP and MP are unchanged.");
    }
    public String setDifficulty(String difficulty) {
        if(!Set.of("STANDARD","STORY").contains(difficulty))throw new IllegalArgumentException("Unknown difficulty");
        if(!canShop())return "Change difficulty at Survivor Camp outside combat.";
        GameState next=copy();next.getCampaign().setDifficulty(difficulty);
        return record(next,"Difficulty changed to "+difficulty+". Rewards and quest progress stay unchanged.");
    }
    public boolean canShop() { var state=getCurrentState();return state.getCurrentLocation().equals("Survivor Camp")&&state.getCampaign().getEnemy()==null&&!state.getCampaign().epilogueClosed(); }
    public String buyItem(String id) {
        if(!canShop())return "The market is at Survivor Camp. Finish combat before trading.";
        GameState next=copy();var entry=ItemCatalog.entry(id);
        if(entry==null||!ItemCatalog.shop().contains(entry))return "This item is not for sale.";
        if(entry.item().getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(next,id))return "You already own this equipment.";
        if(next.getCharacter().getGold()<WorldService.price(next,entry))return "Not enough gold.";
        int price=WorldService.price(next,entry);
        next.getCharacter().setGold(next.getCharacter().getGold()-price);
        inventory.addItem(next.getInventory(),ItemCatalog.stack(id,1));
        return record(next,"Bought "+entry.item().getName()+" for "+price+" gold.");
    }
    public java.util.Set<String> gallery(){var all=saves.gallery();if(currentState!=null)all.addAll(BondService.achievements(currentState));return all;}
    public String companion(String action,String id){GameState next=copy();String message=BondService.manage(next,action,id);next.setCurrentStorySummary(message);next.setCurrentChoices(story.choices(next));return record(next,message);}
    public String craft(String id){GameState next=copy();return record(next,BondService.craft(next,id));}
    public String challenge(String id){GameState next=copy();String message=BondService.challenge(next,id);next.setCurrentStorySummary(message);next.setCurrentChoices(story.choices(next));return record(next,message);}

}
