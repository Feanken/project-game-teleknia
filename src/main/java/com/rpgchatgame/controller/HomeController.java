package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.ui.PixelAssets;
import com.rpgchatgame.util.Constants;
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

        Label eyebrow = new Label("T E L E K N I A   /   THE CINDER CROWN");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("ASHES OF THE\nNORTHERN GATE");
        title.getStyleClass().add("hero-title");
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label subtitle = new Label("A village in ashes. A trail into the unknown. Your story begins here.");
        subtitle.getStyleClass().add("subtitle");

        Button startButton = primaryButton("Start New Story");
        startButton.setOnAction(event -> {
            newAdventure();
        });

        Button continueButton = secondaryButton("Continue Story");
        continueButton.setDisable(!gameService.hasSavedGame());
        continueButton.setOnAction(event -> app.runSafely(() -> {
            gameService.continueStory().ifPresentOrElse(
                    state -> app.showStory(),
                    () -> app.showSaves(false, "")
            );
        }));

        Button inventoryButton = secondaryButton("Inventory");
        inventoryButton.setDisable(!gameService.hasSavedGame() && !gameService.hasCurrentGame());
        inventoryButton.setOnAction(event -> app.runSafely(app::showInventory));

        Button profileButton = secondaryButton("Character Profile");
        profileButton.setDisable(!gameService.hasSavedGame() && !gameService.hasCurrentGame());
        profileButton.setOnAction(event -> app.runSafely(app::showProfile));

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
        Label footer = new Label("VERSION " + Constants.APP_VERSION + "   /   OFFLINE ADVENTURE   /   AUTO SAVE");
        footer.getStyleClass().add("item-meta");
        Button worldButton = secondaryButton("World Map & Quest Journal");
        worldButton.setDisable(!gameService.hasSavedGame() && !gameService.hasCurrentGame());
        worldButton.setOnAction(event -> app.runSafely(app::showAdventure));
        menuCard.getChildren().add(worldButton);
        hero.getChildren().addAll(eyebrow, scenery, title, subtitle, menuCard, footer);
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(hero);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");
        root.setCenter(scroll);
        return root;
    }

    private void newAdventure() {
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("New Adventure");
        if (app.getAudio() != null) dialog.initOwner(javafx.stage.Window.getWindows().stream().filter(javafx.stage.Window::isShowing).findFirst().orElse(null));
        var start = new javafx.scene.control.ButtonType("Begin adventure", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(start, javafx.scene.control.ButtonType.CANCEL);
        var name = new javafx.scene.control.TextField("Kael");
        name.getStyleClass().add("form-field");
        var klass = new javafx.scene.control.ComboBox<String>();
        klass.getItems().addAll("Fighter", "Rogue", "Mage"); klass.setValue("Fighter"); klass.setId("character-class");
        Label details = new Label(); details.setWrapText(true); details.setMaxWidth(440);
        Runnable update = () -> details.setText(switch (klass.getValue()) {
            case "Rogue" -> "88 HP / 30 MP. High agility. Shadow Strike combines weapon power and agility; attacks on every fourth combat turn can critically strike.";
            case "Mage" -> "80 HP / 55 MP. High intelligence. Ember Surge ignores armor. Mend restores more HP.";
            default -> "100 HP / 30 MP. Strong defense. Power Strike doubles your equipped attack before enemy armor.";
        });
        klass.valueProperty().addListener((o,b,a) -> update.run()); update.run();
        var difficulty = new javafx.scene.control.ComboBox<String>();
        difficulty.getItems().addAll("STANDARD", "STORY"); difficulty.setValue("STANDARD"); difficulty.setId("new-difficulty");
        difficulty.setConverter(new javafx.util.StringConverter<>() {
            public String toString(String value) { return com.rpgchatgame.util.UiText.tr(value.equals("STORY") ? "Story difficulty" : "Standard difficulty"); }
            public String fromString(String value) { return value; }
        });
        Label notice = new Label("This replaces autosave only. Manual slots stay safe.");
        notice.setWrapText(true); notice.setMaxWidth(440);
        var tutorial = new javafx.scene.control.CheckBox("Show the beginner tutorial");
        tutorial.setSelected(!app.getSettings().tutorialSeen());
        VBox form = new VBox(12, new Label("Character name (1-24 characters)"), name, new Label("Class"), klass, details, new Label("Difficulty"), difficulty, new Label("Story reduces incoming damage and gold penalties. Standard keeps the normal combat rules."), notice, tutorial);
        form.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        dialog.getDialogPane().lookupButton(start).disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> name.getText().isBlank() || name.getText().trim().length() > 24, name.textProperty()));
        var localizedCancel = new javafx.scene.control.ButtonType(com.rpgchatgame.util.UiText.tr("Cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().set(1, localizedCancel);
        dialog.setTitle(com.rpgchatgame.util.UiText.tr("New Adventure"));
        com.rpgchatgame.ui.UiLocalization.apply(dialog.getDialogPane());
        klass.setOnAction(event -> { update.run(); com.rpgchatgame.ui.UiLocalization.apply(dialog.getDialogPane()); });
        dialog.setResultConverter(type -> type == start);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(ok -> app.runSafely(() -> {
            gameService.startNewStory(name.getText(), klass.getValue(), difficulty.getValue()); app.showStory();
            if (tutorial.isSelected()) app.showHelp();
        }));
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
