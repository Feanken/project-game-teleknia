package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.ui.*;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class SettingsController {
    private final MainApp app;
    public SettingsController(MainApp app){this.app=app;}
    private Label text(String value){Label label=new Label(value);label.setWrapText(true);return label;}
    public Parent createView(){
        var settings=app.getSettings();
        ComboBox<String> language=new ComboBox<>();language.getItems().addAll("en","id");language.setValue(settings.language());language.setId("language-setting");
        language.setConverter(new javafx.util.StringConverter<>(){
            public String toString(String value){return "id".equals(value)?"Bahasa Indonesia":"English";}public String fromString(String value){return value;}
        });
        language.setOnAction(event->app.runSafely(()->{settings.setLanguage(language.getValue());app.applyPreferences();app.showSettings();}));
        ComboBox<Integer> scale=new ComboBox<>();scale.getItems().addAll(90,100,110,120);scale.setValue((int)Math.round(settings.textScale()*100));scale.setId("text-size-setting");
        scale.setConverter(new javafx.util.StringConverter<>(){public String toString(Integer value){return value+"%";}public Integer fromString(String value){return Integer.parseInt(value.replace("%",""));}});
        scale.setOnAction(event->app.runSafely(()->{settings.setTextScale(scale.getValue()/100.0);app.applyPreferences();}));
        CheckBox fullscreen=new CheckBox("Fullscreen");fullscreen.setSelected(settings.fullscreen());fullscreen.setId("fullscreen-setting");
        fullscreen.setOnAction(event->app.runSafely(()->app.setFullscreen(fullscreen.isSelected())));
        CheckBox motion=new CheckBox("Reduce animations");motion.setId("reduced-motion-setting");motion.setSelected(settings.reducedMotion());motion.setOnAction(e->app.runSafely(()->settings.setReducedMotion(motion.isSelected())));
        Button back=new Button("Back");back.getStyleClass().addAll("game-button","secondary-button");back.setOnAction(event->app.runSafely(app::back));
        VBox content=new VBox(18,PixelAssets.heading("journal","PREFERENCES","Display, language and audio settings apply to every save."),
                text("Language"),language,text("Text size"),scale,fullscreen,motion,text("Audio"),new AudioControls(app.getAudio()),
                text("Shortcuts"),text("F1 Help / F5 Save slots / F9 Load slots / F11 Fullscreen / Esc Menu or Back"),
                text("1-9, 0 select the visible action cards. Esc leaves an NPC conversation. Tab and Enter navigate controls."),
                text("Changes are saved automatically. Story narration and lore remain in English."),back);
        content.setPadding(new Insets(24));
        ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.getStyleClass().add("plain-scroll");
        BorderPane root=new BorderPane(scroll);root.getStyleClass().add("app-root");return root;
    }
}
