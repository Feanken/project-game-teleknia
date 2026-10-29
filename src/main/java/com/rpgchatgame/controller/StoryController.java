package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.Choice;
import com.rpgchatgame.model.GameState;
import com.rpgchatgame.model.StoryLog;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.service.AudioService;
import com.rpgchatgame.ui.StatusPanel;
import com.rpgchatgame.ui.PixelAssets;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StoryController {
    private final MainApp app;
    private final GameService gameService;
    private final VBox chatLog = new VBox(12);
    private final javafx.scene.layout.TilePane choicesBox = new javafx.scene.layout.TilePane(10, 8);
    private final TextField customActionField = new TextField();
    private final StatusPanel statusPanel = new StatusPanel();

    public StoryController(MainApp app, GameService gameService) {
        this.app = app;
        this.gameService = gameService;
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(navBar("Story"));

        chatLog.setPadding(new Insets(18));
        chatLog.getStyleClass().add("chat-log");

        ScrollPane scrollPane = new ScrollPane(chatLog);
        scrollPane.setFitToWidth(true);
        scrollPane.setMinHeight(100);
        scrollPane.getStyleClass().add("chat-scroll");

        VBox storyArea = new VBox(14, scrollPane, actionPanel());
        storyArea.setPadding(new Insets(22));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox rightPanel = new VBox(14);
        boolean inCombat = gameService.getCurrentState().getCampaign().getEnemy() != null;
        statusPanel.setCombatMode(inCombat);
        if (inCombat) rightPanel.getChildren().add(enemyCard());
        rightPanel.getChildren().add(statusPanel);
        rightPanel.getChildren().add(locationCard());
        rightPanel.setPadding(new Insets(22, 22, 22, 0));
        rightPanel.setPrefWidth(310);

        root.setCenter(storyArea);
        ScrollPane sideScroll = new ScrollPane(rightPanel);
        sideScroll.setFitToWidth(true);
        sideScroll.setPrefWidth(310);
        sideScroll.getStyleClass().add("plain-scroll");
        root.setRight(sideScroll);

        refresh();
        javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1));
        return root;
    }

    private HBox navBar(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("screen-title");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox nav = new HBox(10, title, spacer, navButton("World", app::showAdventure), navButton("Home", app::showHome), navButton("Inventory", app::showInventory), navButton("Profile", app::showProfile));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(16, 22, 12, 22));
        nav.getStyleClass().add("top-nav");
        return nav;
    }

    private VBox actionPanel() {
        choicesBox.getStyleClass().add("choices-box");
        choicesBox.setPrefColumns(2);
        choicesBox.prefTileWidthProperty().bind(choicesBox.widthProperty().subtract(10).divide(2));
        ScrollPane actions = new ScrollPane(choicesBox);
        actions.setFitToWidth(true);
        actions.setMinHeight(190);
        actions.setPrefHeight(210);
        actions.setMaxHeight(210);
        actions.getStyleClass().add("plain-scroll");
        customActionField.setPromptText("Type attack, guard, heal, rest, or help...");
        customActionField.getStyleClass().add("custom-action");
        customActionField.setOnAction(event -> app.runSafely(this::submitCustomAction));

        Button sendButton = new Button("Send");
        sendButton.getStyleClass().addAll("game-button", "primary-button", "send-button");
        sendButton.setOnAction(event -> app.runSafely(this::submitCustomAction));

        HBox customRow = new HBox(10, customActionField, sendButton);
        HBox.setHgrow(customActionField, Priority.ALWAYS);

        VBox panel = new VBox(12, actions, customRow);
        panel.getStyleClass().addAll("card", "action-panel");
        return panel;
    }

    private VBox enemyCard() {
        var state = gameService.getCurrentState();
        var enemy = state.getCampaign().getEnemy();
        Label name = new Label(enemy.getName()); name.getStyleClass().add("panel-title");
        Label health = new Label("HP " + enemy.getHp() + " / " + enemy.getMaxHp());
        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar((double) enemy.getHp() / enemy.getMaxHp());
        bar.getStyleClass().add("hp-bar"); bar.setMaxWidth(Double.MAX_VALUE);
        var next = com.rpgchatgame.service.CombatService.intent(state);
        Label intent = new Label("Next: " + com.rpgchatgame.util.UiText.tr(next.name()));
        intent.setTooltip(new javafx.scene.control.Tooltip(next.description()));
        intent.setWrapText(true);
        VBox card = new VBox(10, new Label("IN COMBAT / TURN " + (state.getCampaign().getTurn() + 1)), name, health, bar, intent);
        card.getStyleClass().addAll("card", "enemy-card");
        Label effects = new Label(com.rpgchatgame.service.CombatService.effects(state.getCampaign().getEnemyEffects())); effects.setWrapText(true);
        if (!state.getCampaign().getEnemyEffects().isEmpty()) card.getChildren().add(effects);
        if (enemy.getId().equals("warden")) card.getChildren().add(new Label(enemy.getHp() <= enemy.getMaxHp() / 2 ? "Phase II" : "Phase I"));
        return card;
    }

    private VBox locationCard() {
        GameState state = gameService.getCurrentState();
        Label location = new Label(state.getCurrentLocation());
        location.setWrapText(true);
        location.getStyleClass().add("panel-title");
        Label quest = new Label(state.getCurrentQuest());
        quest.setWrapText(true);
        Label chapter = new Label(state.getCampaign().chapter()); chapter.setWrapText(true);
        VBox card = new VBox(10, PixelAssets.view(PixelAssets.location(state.getCurrentLocation()), 2), chapter, new Label("CURRENT LOCATION"), location, new Label("ACTIVE QUEST"), quest);
        card.getStyleClass().addAll("card", "location-card");
        return card;
    }

    private void refresh() {
        GameState state = gameService.getCurrentState();
        statusPanel.update(state.getCharacter());
        if (state.getCampaign().getEnemy() != null && !state.getCampaign().getPlayerEffects().isEmpty()) {
            Label effects = new Label(com.rpgchatgame.service.CombatService.effects(state.getCampaign().getPlayerEffects())); effects.setWrapText(true);
            statusPanel.getChildren().add(3, effects);
        }
        chatLog.getChildren().clear();
        int first = Math.max(0, state.getStoryLogs().size() - 80);
        if (first > 0) chatLog.getChildren().add(new Label("Showing the latest 80 messages. Full history remains saved."));
        for (StoryLog log : state.getStoryLogs().subList(first, state.getStoryLogs().size())) {
            chatLog.getChildren().add(messageBubble(log));
        }
        choicesBox.getChildren().clear();
        for (Choice choice : state.getCurrentChoices()) {
            Button choiceButton = new Button(choice.getLabel());
            choiceButton.getStyleClass().addAll("choice-button", "game-button");
            choiceButton.setMaxWidth(Double.MAX_VALUE);
            String tip = "";
            if (state.getCampaign().getEnemy() != null) {
                tip = com.rpgchatgame.util.UiText.tr(com.rpgchatgame.service.CombatService.unavailable(state, choice.getAction()));
                choiceButton.setDisable(!tip.isBlank());
                var skill = com.rpgchatgame.service.SkillCatalog.find(state, choice.getAction());
                if (skill != null) tip += (tip.isBlank() ? "" : "\n") + com.rpgchatgame.util.UiText.tr(skill.description());
            }
            if (!tip.isBlank()) choiceButton.setTooltip(new javafx.scene.control.Tooltip(tip));
            choiceButton.setOnAction(event -> app.runSafely(() -> submitAction(choice.getAction())));
            choicesBox.getChildren().add(choiceButton);
        }
    }

    private VBox messageBubble(StoryLog log) {
        Label speaker = new Label(speakerName(log.getSpeaker()));
        speaker.getStyleClass().add("bubble-speaker");
        if (log.getSpeaker() == StoryLog.Speaker.PLAYER) speaker.getProperties().put("literal", true);
        Label message = new Label(log.getMessage());
        message.getProperties().put("literal", true);
        message.setWrapText(true);
        String sprite = switch (log.getSpeaker()) {
            case GAME_MASTER -> "game_master";
            case PLAYER -> "kael";
            case SYSTEM -> "journal";
        };
        HBox content = new HBox(12, PixelAssets.view(sprite), message);
        HBox.setHgrow(message, Priority.ALWAYS);
        message.setMaxWidth(Double.MAX_VALUE);
        VBox bubble = new VBox(8, speaker, content);
        bubble.getStyleClass().add("message-bubble");
        if (log.getSpeaker() == StoryLog.Speaker.PLAYER) {
            bubble.getStyleClass().add("player-bubble");
        } else if (log.getSpeaker() == StoryLog.Speaker.SYSTEM) {
            bubble.getStyleClass().add("system-bubble");
        } else {
            bubble.getStyleClass().add("gm-bubble");
        }
        return bubble;
    }

    private String speakerName(StoryLog.Speaker speaker) {
        return switch (speaker) {
            case GAME_MASTER -> "Game Master";
            case PLAYER -> gameService.getCurrentState().getCharacter().getName();
            case SYSTEM -> "System";
        };
    }

    private void submitCustomAction() {
        String action = customActionField.getText().trim();
        if (!action.isBlank()) {
            customActionField.clear();
            submitAction(action);
        }
    }

    private void submitAction(String action) {
        int previousLevel = gameService.getCurrentState().getCharacter().getLevel();
        int potionsBefore = gameService.getCurrentState().getInventory().stream().filter(i -> i.getItem().getType() == com.rpgchatgame.model.Item.ItemType.CONSUMABLE).mapToInt(com.rpgchatgame.model.InventoryItem::getQuantity).sum();
        var response = gameService.performAction(action);
        int potionsAfter = gameService.getCurrentState().getInventory().stream().filter(i -> i.getItem().getType() == com.rpgchatgame.model.Item.ItemType.CONSUMABLE).mapToInt(com.rpgchatgame.model.InventoryItem::getQuantity).sum();
        AudioService.Sound sound;
        if (gameService.getCurrentState().getCharacter().getLevel() > previousLevel) {
            sound = AudioService.Sound.LEVEL_UP;
        } else if (potionsAfter < potionsBefore) {
            sound = AudioService.Sound.POTION;
        } else if (response.getHpChange() < 0) {
            sound = AudioService.Sound.DAMAGE;
        } else if (response.getGoldChange() > 0 || !response.getItemsGained().isEmpty()) {
            sound = AudioService.Sound.REWARD;
        } else {
            sound = AudioService.Sound.ACTION;
        }
        app.getAudio().play(sound);
        app.showStory();
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "nav-button");
        button.setOnAction(event -> app.runSafely(action));
        return button;
    }
}
