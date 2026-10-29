package com.rpgchatgame.ui;

import com.rpgchatgame.model.Character;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class StatusPanel extends VBox {
    private final Label levelLabel = new Label();
    private final Label goldLabel = new Label();
    private final Label hpLabel = new Label();
    private final Label mpLabel = new Label();
    private final Label expLabel = new Label();
    private final ProgressBar hpBar = new ProgressBar();
    private final ProgressBar mpBar = new ProgressBar();
    private final ProgressBar expBar = new ProgressBar();

    public StatusPanel() {
        getStyleClass().addAll("card", "status-panel");
        setSpacing(12);
        setPadding(new Insets(18));

        Label title = new Label("ADVENTURER");
        title.getStyleClass().add("panel-title");
        hpBar.getStyleClass().add("hp-bar");
        mpBar.getStyleClass().add("mp-bar");
        expBar.getStyleClass().add("exp-bar");
        hpBar.setMaxWidth(Double.MAX_VALUE);
        mpBar.setMaxWidth(Double.MAX_VALUE);
        expBar.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(
                PixelAssets.view("kael"),
                title,
                levelLabel,
                goldLabel,
                hpLabel,
                hpBar,
                mpLabel,
                mpBar,
                expLabel,
                expBar
        );
    }

    public void update(Character character) {
        levelLabel.setText("Level " + character.getLevel() + " " + character.getCharacterClass());
        goldLabel.setText("GOLD  " + character.getGold());
        hpLabel.setText("HP: " + character.getHp() + " / " + character.getMaxHp());
        mpLabel.setText("MP: " + character.getMp() + " / " + character.getMaxMp());
        expLabel.setText("EXP: " + character.getExp() + " / 100");
        hpBar.setProgress(ratio(character.getHp(), character.getMaxHp()));
        mpBar.setProgress(ratio(character.getMp(), character.getMaxMp()));
        expBar.setProgress(ratio(character.getExp(), 100));
    }

    private double ratio(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, (double) value / max));
    }
}
