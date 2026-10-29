package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.GameState;
import com.rpgchatgame.model.InventoryItem;
import com.rpgchatgame.model.Item;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.service.InventoryQuery;
import com.rpgchatgame.util.UiText;
import javafx.scene.control.ComboBox;
import com.rpgchatgame.ui.StatusPanel;
import com.rpgchatgame.ui.PixelAssets;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class InventoryController {
    private final MainApp app;
    private final GameService gameService;
    private final ComboBox<String> filter = new ComboBox<>();
    private final ComboBox<String> sorting = new ComboBox<>();
    private final VBox list = new VBox(12);
    private final Label feedback = new Label();
    private final StatusPanel statusPanel = new StatusPanel();

    public InventoryController(MainApp app, GameService gameService) {
        this.app = app;
        this.gameService = gameService;
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(navBar("Inventory"));

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("plain-scroll");

        filter.getItems().addAll("All items", "WEAPON", "ARMOR", "ACCESSORY", "CONSUMABLE", "QUEST");
        filter.setValue("All items"); filter.setId("inventory-filter");
        sorting.getItems().addAll("Name", "Rarity", "Quantity"); sorting.setValue("Name"); sorting.setId("inventory-sort");
        javafx.util.StringConverter<String> converter = new javafx.util.StringConverter<>() {
            public String toString(String value) { return UiText.tr(value); }
            public String fromString(String value) { return value; }
        };
        filter.setConverter(converter); sorting.setConverter(converter);
        filter.setOnAction(event -> refresh()); sorting.setOnAction(event -> refresh());
        HBox controls = new HBox(12, new Label("Filter"), filter, new Label("Sort"), sorting);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(14, PixelAssets.heading("backpack", "TRAVELER'S SATCHEL", "Supplies, relics and the things you carry."), controls, feedback, scrollPane);
        content.setPadding(new Insets(22));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox side = new VBox(18, statusPanel);
        side.setPadding(new Insets(22, 22, 22, 0));
        side.setPrefWidth(310);

        root.setCenter(content);
ScrollPane sideScroll = new ScrollPane(side); sideScroll.setFitToWidth(true); sideScroll.setPrefWidth(310); sideScroll.getStyleClass().add("plain-scroll");
        root.setRight(sideScroll);
        refresh();
        return root;
    }

    private void refresh() {
        GameState state = gameService.getCurrentState();
        statusPanel.update(state.getCharacter());
        list.getChildren().clear();
        feedback.getStyleClass().setAll("feedback-label");
        if (state.getInventory().isEmpty()) {
            list.getChildren().add(PixelAssets.heading("backpack", "Your satchel is empty", "Explore the world to discover supplies."));
        }
        Item.ItemType type = filter.getValue().equals("All items") ? null : Item.ItemType.valueOf(filter.getValue());
        InventoryQuery.Sort order = switch (sorting.getValue()) { case "Rarity" -> InventoryQuery.Sort.RARITY; case "Quantity" -> InventoryQuery.Sort.QUANTITY; default -> InventoryQuery.Sort.NAME; };
        var items = InventoryQuery.select(state.getInventory(), type, order);
        if (items.isEmpty() && !state.getInventory().isEmpty()) list.getChildren().add(PixelAssets.heading("backpack", "No matching items", "Change the filter to see other items."));
        for (InventoryItem inventoryItem : items) {
            list.getChildren().add(itemCard(inventoryItem));
        }
        app.refreshUi();
    }

    private VBox itemCard(InventoryItem inventoryItem) {
        Item item = inventoryItem.getItem();
        Label name = new Label(item.getName() + " x" + inventoryItem.getQuantity());
        name.getStyleClass().add("item-title");
        Label meta = new Label(UiText.tr(item.getType().toString()) + " - " + UiText.tr(item.getRarity().toString()));
        meta.getStyleClass().add("item-meta");
        Label description = new Label(item.getDescription());
        description.setWrapText(true);
        Label effect = new Label(item.getEffect());
        effect.getStyleClass().add("item-effect");
        effect.setWrapText(true);
        effect.setTooltip(new javafx.scene.control.Tooltip(com.rpgchatgame.util.UiText.tr(item.getEffect())));

        var state = gameService.getCurrentState();
        boolean equipment = item.getType() == Item.ItemType.WEAPON || item.getType() == Item.ItemType.ARMOR || item.getType() == Item.ItemType.ACCESSORY;
        boolean equipped = state.getCampaign().getWeapon().equals(item.getId()) || state.getCampaign().getArmor().equals(item.getId()) || state.getCampaign().getAccessory().equals(item.getId());
        Button useButton = new Button(equipped ? "Equipped" : equipment ? "Equip" : "Use");
        useButton.getStyleClass().addAll("game-button", "secondary-button");
        useButton.setDisable(equipped || item.getType() == Item.ItemType.QUEST || state.getCampaign().getEnemy() != null || !state.getCampaign().getEnding().isBlank());
        useButton.setOnAction(event -> app.runSafely(() -> {
            int before = gameService.getCurrentState().getInventory().stream().filter(i -> i.getItem().getId().equals(item.getId())).mapToInt(InventoryItem::getQuantity).sum();
            feedback.setText(equipment ? gameService.equipItem(item.getId()) : gameService.useItem(item.getId()));
            int after = gameService.getCurrentState().getInventory().stream().filter(i -> i.getItem().getId().equals(item.getId())).mapToInt(InventoryItem::getQuantity).sum();
            if (after < before) app.getAudio().play(com.rpgchatgame.service.AudioService.Sound.POTION);
            refresh();
        }));

        HBox header = new HBox(12, PixelAssets.view(PixelAssets.item(item.getId())), name, spacer(), useButton);
        header.setAlignment(Pos.CENTER_LEFT);
        Label comparison = new Label(InventoryQuery.comparison(state, item.getId()));
        comparison.getStyleClass().add("item-effect"); comparison.setWrapText(true);
        VBox card = new VBox(8, header, meta, description, effect);
        if (!comparison.getText().isBlank()) card.getChildren().add(comparison);
        card.getStyleClass().addAll("card", "inventory-card", "rarity-" + item.getRarity().name().toLowerCase());
        return card;
    }

    private HBox navBar(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("screen-title");
        HBox nav = new HBox(10, title, spacer(), navButton("World", app::showAdventure), navButton("Home", app::showHome), navButton("Story", app::showStory), navButton("Profile", app::showProfile));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(16, 22, 12, 22));
        nav.getStyleClass().add("top-nav");
        return nav;
    }

    private HBox spacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "nav-button");
        button.setOnAction(event -> app.runSafely(action));
        return button;
    }
}
