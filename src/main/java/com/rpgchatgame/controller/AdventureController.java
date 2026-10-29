package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.ui.PixelAssets;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/** World travel, quest milestones and a camp-only market, sharing the campaign action gates. */
public class AdventureController {
    private final MainApp app;
    private final GameService game;
    private final int selectedTab;
    public AdventureController(MainApp app,GameService game) { this(app,game,0); }
    public AdventureController(MainApp app,GameService game,int selectedTab) { this.app=app;this.game=game;this.selectedTab=selectedTab; }
    public Parent createView() {
        GameState state=game.getCurrentState();
        BorderPane root=new BorderPane();root.getStyleClass().add("app-root");
        HBox spacer=new HBox();HBox.setHgrow(spacer,Priority.ALWAYS);
        Label title=new Label("Adventure");title.getStyleClass().add("screen-title");
        HBox nav=new HBox(10,title,spacer,button("Home",app::showHome),button("Story",app::showStory),button("Inventory",app::showInventory),button("Profile",app::showProfile));
        nav.setAlignment(Pos.CENTER_LEFT);nav.setPadding(new Insets(16,22,12,22));nav.getStyleClass().add("top-nav");root.setTop(nav);
        TabPane tabs=new TabPane();tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(new Tab("WORLD MAP",scroll(map(state))),new Tab("QUEST JOURNAL",scroll(journal(state))),new Tab("CAMP MARKET",scroll(market(state))));
        tabs.getSelectionModel().select(selectedTab);root.setCenter(tabs);return root;
    }
    private ScrollPane scroll(Parent content) {
        ScrollPane pane=new ScrollPane(content);pane.setFitToWidth(true);pane.getStyleClass().add("plain-scroll");return pane;
    }
    private Button button(String text,Runnable action) {
        Button button=new Button(text);button.getStyleClass().addAll("game-button","nav-button");
        button.setOnAction(event->app.runSafely(action));return button;
    }
    private VBox section(String sprite,String title,String subtitle) {
        VBox box=new VBox(16,PixelAssets.heading(sprite,title,subtitle));box.setPadding(new Insets(24));return box;
    }
    private Label text(String value) { Label label=new Label(value);label.setWrapText(true);return label; }
    private VBox map(GameState state) {
        VBox box=section("old_map",state.getCampaign().chapter(),"Only adjacent, unlocked routes are available. Travel continues in Story.");
        for(String location:LocalStoryGeneratorService.LOCATIONS) {
            String action=switch(location) {
                case "Burned Village"->"go_village";case "Northern Gate"->"go_gate";case "Emberwood Trail"->"go_trail";
                case "Survivor Camp"->"go_camp";case "Emberwood Shrine"->"go_shrine";case "Sunken Ruins"->"go_ruins";default->"go_tower";
            };
            boolean here=state.getCurrentLocation().equals(location);
            boolean reachable=state.getCurrentChoices().stream().anyMatch(choice->choice.getAction().equals(action));
            Button travel=button(here?"You are here":reachable?"Travel":"No route",()->{game.performAction(action);app.showStory();});
            travel.setDisable(here||!reachable);
            String detail=here?"CURRENT LOCATION":state.getCampaign().getVisited().contains(location)?"VISITED":"UNDISCOVERED";
            HBox spacer=new HBox();HBox.setHgrow(spacer,Priority.ALWAYS);
            HBox row=new HBox(16,PixelAssets.heading(PixelAssets.location(location),location,detail),spacer,travel);
            row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("card");box.getChildren().add(row);
        }
        return box;
    }
    private VBox journal(GameState state) {
        var world=state.getCampaign();
        VBox box=section("journal","QUEST JOURNAL",world.chapter()+" / "+state.getCurrentQuest());
        List<String[]> quests=List.of(
                new String[]{"survivors","People Before Ashes","Rescue Mira in Burned Village."},
                new String[]{"wolf","The Northern Road","Defeat the Ash Wolf at Northern Gate."},
                new String[]{"guardian","The Hollow Protector","Defeat the guardian at Emberwood Shrine."},
                new String[]{"sigil","A Fragment of Dawn","Read the runes and restore the shrine's Sun Sigil."},
                new String[]{"archive","The Keeper's Oath","Read the archive in Sunken Ruins."},
                new String[]{"locket_returned","A Brother Remembered","Find the locket in Sunken Ruins and return it to Mira at camp."},
                new String[]{"warden","The Cinder Crown","Defeat the Warden at Cinder Tower, then choose an ending."});
        for(var quest:quests) {
            VBox card=new VBox(6,text((world.has(quest[0])?"[COMPLETE] ":"[OPEN] ")+quest[1]),text(quest[2]));
            card.getStyleClass().add("card");box.getChildren().add(card);
        }
        long victories=List.of("wolf","guardian","warden").stream().filter(world::has).count();
        box.getChildren().add(text("MILESTONES / Encounters won: "+victories+" / 3 | Places visited: "+world.getVisited().size()+" / 7"));
        if(world.has("locket_returned"))box.getChildren().add(text("ACHIEVEMENT / A Kindness Remembered"));
        if(world.getVisited().size()==7)box.getChildren().add(text("ACHIEVEMENT / Roads of Emberwood"));
        if(!world.getEnding().isBlank())box.getChildren().add(text("ENDING UNLOCKED / "+world.getEnding()));
        box.getChildren().add(text("FIELD NOTES"));
        for(String entry:world.getJournal())box.getChildren().add(text("- "+entry));
        box.getChildren().add(text("COMBAT GUIDE / Attack uses your equipped weapon. Class skills cost 6 MP. Arcane Bolt ignores armor for 10 MP. Guard halves damage and restores 6 MP. Mend costs 8 MP. Every third enemy turn is heavy. Potion use spends a turn. Defeat returns you to camp with half HP/MP and costs 20% gold."));
        box.getChildren().add(text("ENDING GUIDE / Purification needs the restored Sun Sigil and keeper's archive. Sealing and claiming are always available after the Warden falls. Returning for missed clues remains possible before choosing an ending."));
        return box;
    }
    private VBox market(GameState state) {
        VBox box=section("backpack","CAMP MARKET","Gold: "+state.getCharacter().getGold()+" / "+(game.canShop()?"The smith and herbalist are ready to trade.":"Visit Survivor Camp outside combat to trade."));
        Label feedback=text("");feedback.getStyleClass().add("feedback-label");box.getChildren().add(feedback);
        for(var entry:ItemCatalog.shop()) {
            Item item=entry.item();
            boolean owned=item.getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(state,item.getId());
            Button buy=button(owned?"Owned":"Buy / "+entry.price()+" gold",()->{
                String message=game.buyItem(item.getId());app.showAdventure(2,message);
            });
            buy.setDisable(!game.canShop()||owned||state.getCharacter().getGold()<entry.price());
            HBox spacer=new HBox();HBox.setHgrow(spacer,Priority.ALWAYS);
            HBox row=new HBox(16,PixelAssets.heading(PixelAssets.item(item.getId()),item.getName(),item.getEffect()),spacer,buy);
            row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("card");box.getChildren().add(row);
        }
        box.getChildren().add(text("Equipment must be equipped from Inventory after buying it. Quest items cannot be bought or sold."));
        return box;
    }
}
