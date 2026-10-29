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
    private final VBox choicesBox = new VBox(10);
    private final TextField customActionField = new TextField();
    private final StatusPanel statusPanel = new StatusPanel();

    public StoryController(MainApp app, GameService gameService) {
        this.app = app;
        this.gameService = gameService;
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(navBar("Story Chat"));

        chatLog.setPadding(new Insets(18));
        chatLog.getStyleClass().add("chat-log");

        ScrollPane scrollPane = new ScrollPane(chatLog);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("chat-scroll");

        VBox storyArea = new VBox(14, scrollPane, actionPanel());
        storyArea.setPadding(new Insets(22));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox rightPanel = new VBox(18, statusPanel, locationCard());
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
        HBox nav = new HBox(10, title, spacer, navButton("Home", app::showHome), navButton("Inventory", app::showInventory), navButton("Profile", app::showProfile));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(16, 22, 12, 22));
        nav.getStyleClass().add("top-nav");
        return nav;
    }

    private VBox actionPanel() {
        choicesBox.getStyleClass().add("choices-box");
        customActionField.setPromptText("Custom action...");
        customActionField.getStyleClass().add("custom-action");
        customActionField.setOnAction(event -> submitCustomAction());

        Button sendButton = new Button("Send");
        sendButton.getStyleClass().addAll("game-button", "primary-button", "send-button");
        sendButton.setOnAction(event -> submitCustomAction());

        HBox customRow = new HBox(10, customActionField, sendButton);
        HBox.setHgrow(customActionField, Priority.ALWAYS);

        VBox panel = new VBox(12, choicesBox, customRow);
        panel.getStyleClass().addAll("card", "action-panel");
        return panel;
    }

    private VBox locationCard() {
        GameState state = gameService.getCurrentState();
        Label location = new Label(state.getCurrentLocation());
        location.getStyleClass().add("panel-title");
        Label quest = new Label(state.getCurrentQuest());
        quest.setWrapText(true);
        VBox card = new VBox(10, PixelAssets.view(PixelAssets.location(state.getCurrentLocation()), 2), new Label("CURRENT LOCATION"), location, new Label("ACTIVE QUEST"), quest);
        card.getStyleClass().addAll("card", "location-card");
        return card;
    }

    private void refresh() {
        GameState state = gameService.getCurrentState();
        statusPanel.update(state.getCharacter());
        chatLog.getChildren().clear();
        for (StoryLog log : state.getStoryLogs()) {
            chatLog.getChildren().add(messageBubble(log));
        }
        choicesBox.getChildren().clear();
        for (Choice choice : state.getCurrentChoices()) {
            Button choiceButton = new Button(choice.getLabel());
            choiceButton.getStyleClass().addAll("choice-button", "game-button");
            choiceButton.setMaxWidth(Double.MAX_VALUE);
            choiceButton.setOnAction(event -> submitAction(choice.getAction()));
            choicesBox.getChildren().add(choiceButton);
        }
    }

    private VBox messageBubble(StoryLog log) {
        Label speaker = new Label(speakerName(log.getSpeaker()));
        speaker.getStyleClass().add("bubble-speaker");
        Label message = new Label(log.getMessage());
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
            case PLAYER -> "Kael";
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
        var response = gameService.performAction(action);
        AudioService.Sound sound;
        if (gameService.getCurrentState().getCharacter().getLevel() > previousLevel) {
            sound = AudioService.Sound.LEVEL_UP;
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
        button.setOnAction(event -> action.run());
        return button;
    }
}
