package com.rpgchatgame.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Native 64 x 64 sprites; integer scaling preserves their pixel grid. */
public final class PixelAssets {
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private PixelAssets() {}

    public static ImageView view(String name) { return view(name, 1); }

    public static ImageView view(String name, int scale) {
        if (scale < 1) throw new IllegalArgumentException("Scale must be positive");
        Image image = CACHE.computeIfAbsent(name, key -> {
            var resource = PixelAssets.class.getResource("/sprites/" + key + ".png");
            if (resource == null) throw new IllegalArgumentException("Missing sprite: " + key);
            return new Image(resource.toExternalForm());
        });
        ImageView view = new ImageView(image);
        view.setFitWidth(64 * scale);
        view.setFitHeight(64 * scale);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        view.setAccessibleText(name.replace('_', ' '));
        return view;
    }

    public static String location(String location) {
        if (location == null) return "village";
        if (location.contains("Camp")) return "campfire";
        if (location.contains("Ruins") || location.contains("Tower")) return "gate";
        if (location.contains("Gate")) return "gate";
        if (location.contains("Emberwood") || location.contains("Shrine")) return "shrine";
        return "village";
    }

    public static String item(String id) {
        return switch (id) {
            case "rusty_sword", "small_potion", "old_map", "ash_fang" -> id;
            case "iron_sword", "ember_blade" -> "rusty_sword";
            case "mana_potion" -> "mana";
            case "silver_locket", "sun_sigil", "duelist_charm" -> "experience";
            case "warding_charm" -> "mana";
            default -> "backpack";
        };
    }

    public static HBox heading(String sprite, String title, String subtitle) {
        Label name = new Label(title);
        name.getStyleClass().add("panel-title");
        Label detail = new Label(subtitle);
        detail.getStyleClass().add("item-meta");
        detail.setWrapText(true);
        HBox row = new HBox(14, view(sprite), new javafx.scene.layout.VBox(6, name, detail));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
