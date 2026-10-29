package com.rpgchatgame.service;

import com.rpgchatgame.model.Character;
import com.rpgchatgame.model.Choice;
import com.rpgchatgame.model.GameState;
import com.rpgchatgame.model.InventoryItem;
import com.rpgchatgame.model.StoryLog;
import com.rpgchatgame.model.StoryResponse;
import com.rpgchatgame.repository.CharacterRepository;
import com.rpgchatgame.repository.DatabaseManager;
import com.rpgchatgame.repository.InventoryRepository;
import com.rpgchatgame.repository.StoryLogRepository;

import java.util.List;
import java.util.Optional;

public class GameService {
    private static final String OPENING_STORY = "You wake up in a burned village. Smoke rises from broken houses, and a strange roar echoes from the northern gate.";

    private final DatabaseManager databaseManager;
    private final CharacterRepository characterRepository;
    private final InventoryRepository inventoryRepository;
    private final StoryLogRepository storyLogRepository;
    private final CharacterService characterService;
    private final InventoryService inventoryService;
    private final StoryGeneratorService storyGeneratorService;

    private GameState currentState;

    public GameService() {
        this.databaseManager = new DatabaseManager();
        this.databaseManager.initialize();
        this.characterRepository = new CharacterRepository(databaseManager);
        this.inventoryRepository = new InventoryRepository(databaseManager);
        this.storyLogRepository = new StoryLogRepository(databaseManager);
        this.characterService = new CharacterService();
        this.inventoryService = new InventoryService();
        this.storyGeneratorService = new LocalStoryGeneratorService();
    }

    public GameState startNewStory() {
        databaseManager.clearAll();
        Character character = characterService.createDefaultCharacter();
        List<InventoryItem> inventory = inventoryService.createStarterInventory();
        List<Choice> choices = initialChoices();

        GameState gameState = new GameState();
        gameState.setCharacter(character);
        gameState.setInventory(inventory);
        gameState.setCurrentStorySummary(OPENING_STORY);
        gameState.setCurrentLocation(character.getCurrentLocation());
        gameState.setCurrentQuest(character.getCurrentQuest());
        gameState.setCurrentChoices(choices);
        gameState.getStoryLogs().add(new StoryLog(StoryLog.Speaker.GAME_MASTER, OPENING_STORY));

        currentState = gameState;
        saveAll();
        return currentState;
    }

    public Optional<GameState> continueStory() {
        Optional<Character> character = characterRepository.load();
        Optional<StoryLogRepository.SavedStoryState> savedStoryState = storyLogRepository.loadGameState();
        if (character.isEmpty() || savedStoryState.isEmpty()) {
            return Optional.empty();
        }

        GameState gameState = new GameState();
        gameState.setCharacter(character.get());
        gameState.setInventory(inventoryRepository.loadAll());
        gameState.setStoryLogs(storyLogRepository.loadAll());
        gameState.setCurrentStorySummary(savedStoryState.get().summary());
        gameState.setCurrentLocation(savedStoryState.get().location());
        gameState.setCurrentQuest(savedStoryState.get().quest());
        gameState.setCurrentChoices(savedStoryState.get().choices());
        currentState = gameState;
        return Optional.of(currentState);
    }

    public GameState getCurrentState() {
        if (currentState == null) {
            currentState = continueStory().orElseGet(this::startNewStory);
        }
        return currentState;
    }

    public StoryResponse performAction(String action) {
        GameState gameState = getCurrentState();
        StoryLog playerLog = new StoryLog(StoryLog.Speaker.PLAYER, action);
        gameState.getStoryLogs().add(playerLog);
        storyLogRepository.append(playerLog);

        StoryResponse response = storyGeneratorService.generateNextScene(gameState, action);
        applyStoryResponse(gameState, response);

        StoryLog gmLog = new StoryLog(StoryLog.Speaker.GAME_MASTER, response.getNarration());
        gameState.getStoryLogs().add(gmLog);
        storyLogRepository.append(gmLog);

        String systemMessage = buildSystemMessage(response, gameState);
        if (!systemMessage.isBlank()) {
            StoryLog systemLog = new StoryLog(StoryLog.Speaker.SYSTEM, systemMessage);
            gameState.getStoryLogs().add(systemLog);
            storyLogRepository.append(systemLog);
        }

        saveAll();
        return response;
    }

    public String useItem(String itemId) {
        GameState gameState = getCurrentState();
        String message = inventoryService.useItem(gameState.getInventory(), gameState.getCharacter(), itemId);
        StoryLog systemLog = new StoryLog(StoryLog.Speaker.SYSTEM, message);
        gameState.getStoryLogs().add(systemLog);
        storyLogRepository.append(systemLog);
        saveAll();
        return message;
    }

    public boolean hasSavedGame() {
        return characterRepository.load().isPresent();
    }

    private void applyStoryResponse(GameState gameState, StoryResponse response) {
        Character character = gameState.getCharacter();
        String levelMessage = characterService.applyProgress(
                character,
                response.getHpChange(),
                response.getMpChange(),
                response.getExpGain(),
                response.getGoldChange()
        );
        if (response.getNewLocation() != null && !response.getNewLocation().isBlank()) {
            character.setCurrentLocation(response.getNewLocation());
        }
        if (response.getQuestUpdate() != null && !response.getQuestUpdate().isBlank()) {
            character.setCurrentQuest(response.getQuestUpdate());
        }
        for (InventoryItem item : response.getItemsGained()) {
            inventoryService.addItem(gameState.getInventory(), item);
        }
        if (!levelMessage.isBlank()) {
            response.setNarration(response.getNarration() + "\n\n" + levelMessage);
        }
        gameState.setCurrentStorySummary(response.getNarration());
        gameState.setCurrentLocation(character.getCurrentLocation());
        gameState.setCurrentQuest(character.getCurrentQuest());
        gameState.setCurrentChoices(response.getChoices());
    }

    private String buildSystemMessage(StoryResponse response, GameState gameState) {
        StringBuilder builder = new StringBuilder();
        if (response.getHpChange() != 0) {
            builder.append("HP ").append(signed(response.getHpChange())).append(". ");
        }
        if (response.getMpChange() != 0) {
            builder.append("MP ").append(signed(response.getMpChange())).append(". ");
        }
        if (response.getExpGain() > 0) {
            builder.append("EXP +").append(response.getExpGain()).append(". ");
        }
        if (response.getGoldChange() != 0) {
            builder.append("Gold ").append(signed(response.getGoldChange())).append(". ");
        }
        if (!response.getItemsGained().isEmpty()) {
            builder.append("Item gained: ");
            for (InventoryItem item : response.getItemsGained()) {
                builder.append(item.getItem().getName()).append(" x").append(item.getQuantity()).append(" ");
            }
        }
        if (gameState.getCharacter().getHp() <= 0) {
            builder.append("Kael collapses, but the MVP lets you continue from the brink.");
            gameState.getCharacter().setHp(1);
        }
        return builder.toString().trim();
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private void saveAll() {
        GameState gameState = getCurrentState();
        characterRepository.save(gameState.getCharacter());
        inventoryRepository.saveAll(gameState.getInventory());
        storyLogRepository.saveGameState(
                gameState.getCurrentStorySummary(),
                gameState.getCurrentLocation(),
                gameState.getCurrentQuest(),
                gameState.getCurrentChoices()
        );
    }

    private List<Choice> initialChoices() {
        return List.of(
                new Choice("Search the village for survivors", "Search the village for survivors"),
                new Choice("Inspect the northern gate", "Inspect the northern gate"),
                new Choice("Check your inventory", "Check your inventory")
        );
    }
}
