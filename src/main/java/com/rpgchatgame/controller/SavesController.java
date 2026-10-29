package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.ui.PixelAssets;
import com.rpgchatgame.util.UiText;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class SavesController {
    private final MainApp app;private final GameService game;private final boolean saving;private final String message;
    public SavesController(MainApp app,GameService game,boolean saving,String message){this.app=app;this.game=game;this.saving=saving;this.message=message;}
    private Button button(String title,Runnable action){Button button=new Button(title);button.getStyleClass().addAll("game-button","secondary-button");button.setOnAction(event->app.runSafely(action));return button;}
    private Label text(String value){Label label=new Label(value);label.setWrapText(true);return label;}
    public Parent createView(){
        VBox content=new VBox(14,PixelAssets.heading("journal","SAVE MANAGER","Three manual slots and a separate autosave. Backups are kept independently."),text(message));
        content.setPadding(new Insets(22));
        if(!game.hasCurrentGame())content.getChildren().add(text("Start or load a character before making a manual save."));
        for(var slot:game.saveSlots()){
            String title=slot.slot()==0?"Autosave":UiText.format("Manual slot {0}",slot.slot());
            Label name=text(title);name.getStyleClass().add("panel-title");
            String summary=slot.corrupt()?"Damaged save":!slot.occupied()?"Empty slot":UiText.format("{0} / Level {1} {2} / {3}",slot.name(),slot.level(),slot.characterClass(),slot.location());
            Label detail=text(summary);detail.getProperties().put("literal",true);
            if(slot.corrupt()||!slot.occupied())detail.setText(UiText.tr(summary));
            Label date=text(UiText.format("Saved: {0}",date(slot.savedAt())));
            HBox actions=new HBox(10);
            if(slot.slot()>0){
                Button save=button("Save here",()->{
                    if(!slot.occupied()||app.confirm(UiText.format("Save slot {0}?",slot.slot()),"This replaces the selected manual save. Its previous valid state becomes the backup.")){
                        game.saveSlot(slot.slot());app.showSaves(true,UiText.format("Saved to slot {0}.",slot.slot()));
                    }
                });
                save.setDisable(!game.hasCurrentGame()||slot.corrupt());actions.getChildren().add(save);
            }
            Button load=button("Load this save",()->{
                if(!game.hasCurrentGame()||app.confirm("Load this adventure?","Loading replaces the current autosave. Manual saves remain unchanged.")){
                    game.loadSlot(slot.slot()).orElseThrow();app.showStory();
                }
            });load.setDisable(!slot.occupied()||slot.corrupt());actions.getChildren().add(load);
            Button restore=button("Restore backup",()->{
                if(app.confirm("Restore this backup?","The selected slot will be replaced by its last valid backup and loaded. Other manual slots stay unchanged.")){
                    game.restoreBackup(slot.slot());app.showStory();
                }
            });restore.setDisable(!slot.backup());actions.getChildren().add(restore);
            VBox card=new VBox(9,name,detail,date,text(slot.backup()?"Backup available":"No valid backup"),actions);
            card.getStyleClass().add("card");content.getChildren().add(card);
            if(saving&&slot.slot()==1)javafx.application.Platform.runLater(()->actions.getChildren().getFirst().requestFocus());
        }
        content.getChildren().add(button("Back",app::back));
        ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.getStyleClass().add("plain-scroll");
        BorderPane root=new BorderPane(scroll);root.getStyleClass().add("app-root");return root;
    }
    private String date(String timestamp){
        if(timestamp==null||timestamp.isBlank())return "-";
        try{return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss",java.util.Locale.forLanguageTag(UiText.language())).format(Instant.parse(timestamp).atZone(ZoneId.systemDefault()));}
        catch(Exception error){return timestamp;}
    }
}
