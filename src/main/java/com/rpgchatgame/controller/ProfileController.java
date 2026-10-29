package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.Character;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.ui.PixelAssets;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProfileController {
    private final MainApp app;
    private final GameService gameService;

    public ProfileController(MainApp app, GameService gameService) {
        this.app = app;
        this.gameService = gameService;
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(navBar("Character Profile"));

        Character character = gameService.getCurrentState().getCharacter();

        Label name = new Label(character.getName());
        name.getStyleClass().add("profile-name");
        name.getProperties().put("literal", true);
        Label klass = new Label(character.getCharacterClass() + " - Level " + character.getLevel());
        klass.getStyleClass().add("subtitle");

        GridPane stats = new GridPane();
        stats.getStyleClass().add("stats-grid");
        stats.setHgap(14);
        stats.setVgap(14);
        addStat(stats, 0, "EXP", character.getExp() + " / 100");
        addStat(stats, 1, "HP", character.getHp() + " / " + character.getMaxHp());
        addStat(stats, 2, "MP", character.getMp() + " / " + character.getMaxMp());
        addStat(stats, 3, "Attack", String.valueOf(com.rpgchatgame.service.ItemCatalog.attack(gameService.getCurrentState())));
        addStat(stats, 4, "Strength", String.valueOf(character.getStrength()));
        addStat(stats, 5, "Agility", String.valueOf(character.getAgility()));
        addStat(stats, 6, "Intelligence", String.valueOf(character.getIntelligence()));
        addStat(stats, 7, "Defense", String.valueOf(com.rpgchatgame.service.ItemCatalog.defense(gameService.getCurrentState())));
        addStat(stats, 8, "Gold", String.valueOf(character.getGold()));
        addStat(stats, 9, "Current Location", character.getCurrentLocation());
        addStat(stats, 10, "Current Quest", character.getCurrentQuest());

        addStat(stats, 11, "Weapon", com.rpgchatgame.service.ItemCatalog.equippedName(gameService.getCurrentState().getCampaign().getWeapon()));
        addStat(stats, 12, "Armor", com.rpgchatgame.service.ItemCatalog.equippedName(gameService.getCurrentState().getCampaign().getArmor()));
        addStat(stats, 13, "Class skill / 6 MP", com.rpgchatgame.service.LocalStoryGeneratorService.skillName(gameService.getCurrentState()));
        addStat(stats, 14, "Accessory", com.rpgchatgame.service.ItemCatalog.equippedName(gameService.getCurrentState().getCampaign().getAccessory()));
        addStat(stats, 15, "Skill points", String.valueOf(com.rpgchatgame.service.SkillCatalog.points(gameService.getCurrentState())));
        addStat(stats, 16, "Difficulty", gameService.getCurrentState().getCampaign().getDifficulty());
        addStat(stats, 17, "Critical attacks", "Every " + com.rpgchatgame.service.ItemCatalog.criticalPeriod(gameService.getCurrentState()) + " turns, requires Agility 10 or Duelist Charm");
        Button skills = navButton("Manage skills", () -> app.showAdventure(3, ""));
        HBox identity = new HBox(20, PixelAssets.view("kael", 2), new VBox(10, name, klass));
        identity.setAlignment(Pos.CENTER_LEFT);
        VBox profileCard = new VBox(18, identity, skills, stats);
        profileCard.getStyleClass().addAll("card", "profile-card");
        profileCard.setMaxWidth(760);

        BorderPane centerWrap = new BorderPane(profileCard);
        centerWrap.setPadding(new Insets(34));
        BorderPane.setAlignment(profileCard, Pos.TOP_CENTER);
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(centerWrap);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");
        root.setCenter(scroll);
        return root;
    }

    private void addStat(GridPane grid, int index, String label, String value) {
        VBox stat = new VBox(4);
        stat.getStyleClass().add("stat-cell");
        Label key = new Label(label);
        key.getStyleClass().add("stat-key");
        Label val = new Label(value);
        val.setWrapText(true);
        val.getStyleClass().add("stat-value");
        stat.getChildren().addAll(key, val);
        grid.add(stat, index % 2, index / 2);
    }

    private HBox navBar(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("screen-title");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox nav = new HBox(10, title, spacer, navButton("World", app::showAdventure), navButton("Home", app::showHome), navButton("Story", app::showStory), navButton("Inventory", app::showInventory));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(16, 22, 12, 22));
        nav.getStyleClass().add("top-nav");
        return nav;
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "nav-button");
        button.setOnAction(event -> app.runSafely(action));
        return button;
    }
}
