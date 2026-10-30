package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.ui.PixelAssets;
import com.rpgchatgame.util.UiText;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class TutorialController {
    private static final String[][] STEPS={
        {"old_map","Choose your next move","Click MAP at the lower right to unfold the world map. Select a connected node to travel; dark nodes hide unexplored locations. Click blue dialogue to reveal or continue. After reading, use action cards, 1-9, 0 or Tab and Enter. There is no custom chat input."},
        {"rusty_sword","Watch the enemy","Attack uses your equipped weapon. Every third enemy turn is heavy: Guard halves damage and restores mana. Skills cost MP. Potions spend a turn; invalid actions spend none."},
        {"backpack","Equip before fighting","Inventory has All and Quest Items categories and thirty slots per page. Select an item for details or right-click for Equip, Unequip, Use or Remove. Hover for one second to read details. Quest items and crafting materials cannot be discarded. Buying does not auto-equip."},
        {"campfire","Find shelter and supplies","At Survivor Camp you can buy supplies and rest. Your first stay is free. Defeat returns you to camp with half HP/MP and costs 20% gold. Retreat costs up to 5 gold."},
        {"journal","Keep your journey safe","Codex contains the map, quests, market, build, archive, history, companions, arena and gallery. Choose a card to inspect it; use its explicit action button to make changes. Autosave follows committed actions. Saves keeps three manual checkpoints and backups. F1 reopens this guide."}
    };
    private final MainApp app;private final int step;
    public TutorialController(MainApp app,int step){this.app=app;this.step=Math.max(0,Math.min(4,step));}
    private Button button(String title,Runnable action){Button b=new Button(title);b.getStyleClass().addAll("game-button","secondary-button");b.setOnAction(e->app.runSafely(action));return b;}
    private void finish(){app.getSettings().setTutorialSeen(true);app.back();}
    public Parent createView(){
        Label heading=new Label("TRAVELER'S GUIDE");heading.getStyleClass().add("screen-title");
        Label title=new Label(STEPS[step][1]);title.getStyleClass().add("panel-title");title.setWrapText(true);
        Label body=new Label(STEPS[step][2]);body.setWrapText(true);body.setMaxWidth(700);
        Button previous=button("Previous",()->app.showTutorial(step-1));previous.setDisable(step==0);
        Button next=button(step==4?"Finish":"Next",()->{if(step==4)finish();else app.showTutorial(step+1);});
        Button skip=button("Skip tutorial",this::finish);
        VBox content=new VBox(22,heading,new Label(UiText.format("Step {0} of {1}",step+1,5)),PixelAssets.view(STEPS[step][0],2),title,body,new HBox(12,previous,next,skip));
        content.setPadding(new Insets(30));
        ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.getStyleClass().add("plain-scroll");
        BorderPane root=new BorderPane(scroll);root.getStyleClass().add("app-root");return root;
    }
}
