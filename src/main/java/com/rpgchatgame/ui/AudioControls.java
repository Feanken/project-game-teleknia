package com.rpgchatgame.ui;

import com.rpgchatgame.service.AudioService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Persistent footer shared by every game screen. */
public final class AudioControls extends HBox {
    public AudioControls(AudioService audio) {
        super(12);
        getStyleClass().add("audio-controls");
        setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("SOUND");
        title.getStyleClass().add("item-meta");
        ToggleButton mute = new ToggleButton();
        mute.getStyleClass().addAll("game-button", "nav-button");
        mute.setSelected(audio.isMuted());
        mute.setText(audio.isMuted() ? "Unmute" : "Mute");
        mute.setTooltip(new Tooltip("Mute or restore all game audio"));
        mute.setOnAction(event -> {
            audio.setMuted(mute.isSelected());
            mute.setText(mute.isSelected() ? "Unmute" : "Mute");
        });
        Slider music = slider("Music volume", audio.getMusicVolume());
        Slider effects = slider("Sound effect volume", audio.getEffectsVolume());
        music.valueProperty().addListener((o, before, after) -> audio.setMusicVolume(after.doubleValue()));
        effects.valueProperty().addListener((o, before, after) -> audio.setEffectsVolume(after.doubleValue()));
        effects.setOnMouseReleased(event -> audio.play(AudioService.Sound.CLICK));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(title, mute, spacer, new Label("Music"), music, new Label("SFX"), effects);
    }

    private Slider slider(String title, double initial) {
        Slider slider = new Slider(0, 1, initial);
        slider.setPrefWidth(130);
        slider.setMaxWidth(130);
        slider.setBlockIncrement(0.05);
        slider.setAccessibleText(title);
        slider.setTooltip(new Tooltip(title));
        return slider;
    }
}
