package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.ui.PixelAssets;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class HomeController {
    private final MainApp app;
    private final GameService gameService;

    public HomeController(MainApp app, GameService gameService) {
        this.app = app;
        this.gameService = gameService;
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "home-root");

        VBox hero = new VBox(16);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(32));

        Label eyebrow = new Label("T E L E K N I A   /   CHAPTER I");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("ASHES OF THE\nNORTHERN GATE");
        title.getStyleClass().add("hero-title");
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label subtitle = new Label("A village in ashes. A trail into the unknown. Your story begins here.");
        subtitle.getStyleClass().add("subtitle");

        Button startButton = primaryButton("Start New Story");
        startButton.setOnAction(event -> {
            gameService.startNewStory();
            app.showStory();
        });

        Button continueButton = secondaryButton("Continue Story");
        continueButton.setDisable(!gameService.hasSavedGame());
        continueButton.setOnAction(event -> {
            gameService.continueStory().ifPresentOrElse(
                    state -> app.showStory(),
                    () -> {
                        gameService.startNewStory();
                        app.showStory();
                    }
            );
        });

        Button inventoryButton = secondaryButton("Inventory");
        inventoryButton.setOnAction(event -> app.showInventory());

        Button profileButton = secondaryButton("Character Profile");
        profileButton.setOnAction(event -> app.showProfile());

        HBox topButtons = new HBox(14, startButton, continueButton);
        topButtons.setAlignment(Pos.CENTER);
        HBox bottomButtons = new HBox(14, inventoryButton, profileButton);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox menuCard = new VBox(16, topButtons, bottomButtons);
        menuCard.setAlignment(Pos.CENTER);
        menuCard.getStyleClass().add("home-menu");

        HBox scenery = new HBox(0, PixelAssets.view("village", 2), PixelAssets.view("gate", 2), PixelAssets.view("shrine", 2));
        scenery.setAlignment(Pos.CENTER);
        scenery.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        scenery.getStyleClass().add("world-strip");
        Label footer = new Label("SINGLE PLAYER   /   OFFLINE ADVENTURE   /   AUTO SAVE");
        footer.getStyleClass().add("item-meta");
        hero.getChildren().addAll(eyebrow, scenery, title, subtitle, menuCard, footer);
        root.setCenter(hero);
        return root;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "primary-button");
        button.setMinWidth(240);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "secondary-button");
        button.setMinWidth(240);
        return button;
    }
}
