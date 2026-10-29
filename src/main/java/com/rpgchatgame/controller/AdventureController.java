package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.ui.PixelAssets;
import com.rpgchatgame.util.UiText;
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
        tabs.getTabs().addAll(new Tab("WORLD MAP",scroll(map(state))),new Tab("QUEST JOURNAL",scroll(journal(state))),new Tab("CAMP MARKET",scroll(market(state))),new Tab("SKILLS & BUILD",scroll(build(state))),new Tab("CODEX",scroll(codex(state))),new Tab("CHRONICLE",scroll(chronicle(state))));
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
            String action=WorldContent.travelId(location);
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
        VBox box=section("journal","QUEST JOURNAL",UiText.tr(world.chapter())+" / "+state.getCurrentQuest());
        for(var quest:WorldContent.QUESTS.values()) {
            Button track=button("Track quest",()->{game.trackQuest(quest.id());app.showAdventure(1,"");});
            track.setId("track-"+quest.id());track.setDisable(world.has(quest.id()));
            Label title=text((world.has(quest.id())?"[COMPLETE] ":"[OPEN] ")+quest.title());title.getStyleClass().add("panel-title");
            VBox card=new VBox(8,title,text(quest.region()+" / "+quest.description()),track);
            card.getStyleClass().add("card");box.getChildren().add(card);
        }
        long complete=WorldContent.QUESTS.keySet().stream().filter(world::has).count();
        box.getChildren().add(text("Quest milestones: "+complete+" / "+WorldContent.QUESTS.size()+" | Places visited: "+world.getVisited().size()+" / "+WorldContent.LOCATIONS.size()));
        if(world.has("locket_returned"))box.getChildren().add(text("ACHIEVEMENT / A Kindness Remembered"));
        if(world.getVisited().size()==WorldContent.LOCATIONS.size())box.getChildren().add(text("ACHIEVEMENT / Roads Beyond Emberwood"));
        if(!world.getEnding().isBlank())box.getChildren().add(text("ENDING UNLOCKED / "+world.getEnding()));
        box.getChildren().add(text("FIELD NOTES"));
        for(String entry:world.getJournal())box.getChildren().add(text("- "+entry));
        box.getChildren().add(text("COMBAT GUIDE / Attack uses your equipped weapon. Each class has three skills with individual MP costs and cooldowns. Arcane Bolt ignores armor for 10 MP. Guard halves damage and restores 6 MP. Mend costs 8 MP. Every third enemy turn is heavy; other intents include charge and defensive stance. Potion use spends a turn. Defeat returns you to camp with half HP/MP and costs 20% gold."));
        box.getChildren().add(text("ENDING GUIDE / Purification needs the restored Sun Sigil and keeper's archive. Sealing and claiming are always available after the Warden falls. Returning for missed clues remains possible before choosing an ending."));
        return box;
    }
    private VBox codex(GameState state) {
        VBox box=section("journal","CODEX","Discover places, people and creatures by exploring. Unknown entries remain hidden.");
        var known=state.getCampaign().getCodex();
        for(var location:WorldContent.LOCATIONS.values())if(known.contains("location:"+location.id()))box.getChildren().add(codexCard(location.sprite(),location.name(),location.description()));
        for(var npc:WorldContent.NPCS.values())if(known.contains("npc:"+npc.id()))box.getChildren().add(codexCard(npc.sprite(),npc.name(),npc.lore()));
        for(var enemy:WorldContent.MONSTERS.values())if(known.contains("enemy:"+enemy.id()))box.getChildren().add(codexCard(enemy.sprite(),enemy.name(),enemy.lore()));
        int total=WorldContent.LOCATIONS.size()+WorldContent.NPCS.size()+WorldContent.MONSTERS.size();
        box.getChildren().add(text("Discovered: "+known.size()+" / "+total));return box;
    }
    private VBox codexCard(String sprite,String title,String lore){VBox card=new VBox(10,PixelAssets.heading(sprite,title,lore));card.getStyleClass().add("card");return card;}
    private VBox chronicle(GameState state){
        VBox box=section("journal","CHRONICLE","Read-only history of your choices and outcomes. The adventure is controlled entirely through action buttons.");
        var logs=state.getStoryLogs();
        for(var log:logs.subList(Math.max(0,logs.size()-100),logs.size())){
            Label line=text(log.getMessage());line.getProperties().put("literal",true);line.getStyleClass().add("chronicle-entry");box.getChildren().add(line);
        }
        return box;
    }
    private VBox build(GameState state) {
        VBox box=section("experience","SKILLS & BUILD","One skill point per level, including level 1. Every skill works at rank 0; upgrades improve its strength, not cooldown or duration.");
        Label feedback=text("");feedback.getStyleClass().add("feedback-label");box.getChildren().add(feedback);
        box.getChildren().add(text("Skill points: "+SkillCatalog.points(state)));
        ComboBox<String> difficulty=new ComboBox<>();difficulty.getItems().addAll("STANDARD","STORY");difficulty.setValue(state.getCampaign().getDifficulty());difficulty.setId("build-difficulty");
        difficulty.setConverter(new javafx.util.StringConverter<>() {
            public String toString(String value){return UiText.tr(value.equals("STORY")?"Story difficulty":"Standard difficulty");}
            public String fromString(String value){return value;}
        });
        difficulty.setDisable(!game.canShop());
        difficulty.setOnAction(event->app.runSafely(()->app.showAdventure(3,game.setDifficulty(difficulty.getValue()))));
        box.getChildren().addAll(text("Difficulty"),difficulty,text("Story: 35% less incoming damage, 10% gold loss on defeat, retreat costs up to 2 gold. Standard: normal damage, 20% loss, up to 5 gold. Change only at camp; rewards are identical."));
        for(var skill:SkillCatalog.forClass(state.getCharacter().getCharacterClass())) {
            int rank=SkillCatalog.rank(state,skill.id());
            Label title=text(skill.name()+" / Rank "+rank+" / 3");title.getStyleClass().add("panel-title");
            Button upgrade=button("Upgrade / 1 point",()->app.showAdventure(3,game.upgradeSkill(skill.id())));upgrade.setId("upgrade-"+skill.id());
            upgrade.setDisable(SkillCatalog.points(state)<1||rank>=3||state.getCampaign().getEnemy()!=null||state.getCampaign().epilogueClosed());
            VBox card=new VBox(9,title,text(skill.description()),text(skill.mp()+" MP / Cooldown: "+skill.cooldown()+" other actions"),
                    text("Current: "+UiText.tr(SkillCatalog.preview(state,skill,rank))));
            if(rank<3)card.getChildren().add(text("Next rank: "+UiText.tr(SkillCatalog.preview(state,skill,rank+1))));
            card.getChildren().add(upgrade);card.getStyleClass().add("card");box.getChildren().add(card);
        }
        Button reset=button("Reset build / 20 gold",()->{
            if(app.confirm("Reset build?","Refund allocated points for 20 gold. HP, MP and character levels do not change."))app.showAdventure(3,game.resetBuild());
        });reset.setDisable(!game.canShop()||state.getCharacter().getGold()<20||state.getCampaign().getSkillRanks().isEmpty());box.getChildren().add(reset);
        box.getChildren().add(text("STATUS RULES / Damage over time ticks at the end of valid rounds, including its application round, then duration decreases. Stun skips this round's enemy action. Shield absorbs direct damage only. Effects refresh duration and keep the higher potency; they never add stacks. Cooldowns require the shown number of OTHER valid actions. Combat exit clears all effects and cooldowns."));
        box.getChildren().add(text("FATIGUE / After turn 12, enemy direct attacks gain 1 power every two turns. Guard and healing cannot stall a fight forever."));
        return box;
    }
    private VBox market(GameState state) {
        VBox box=section("backpack","CAMP MARKET","Gold: "+state.getCharacter().getGold()+" / "+UiText.tr(game.canShop()?"The smith and herbalist are ready to trade.":"Visit Survivor Camp outside combat to trade."));
        Label feedback=text("");feedback.getStyleClass().add("feedback-label");box.getChildren().add(feedback);
        if(WorldService.reputation(state,"camp")>=3)box.getChildren().add(text("Trusted by Survivor Camp / 20% market discount"));
        for(var entry:ItemCatalog.shop()) {
            Item item=entry.item();
            boolean owned=item.getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(state,item.getId());
            int price=WorldService.price(state,entry);
            Button buy=button(owned?"Owned":"Buy / "+price+" gold",()->{
                String message=game.buyItem(item.getId());app.showAdventure(2,message);
            });
            buy.setDisable(!game.canShop()||owned||state.getCharacter().getGold()<price);
            HBox spacer=new HBox();HBox.setHgrow(spacer,Priority.ALWAYS);
            HBox row=new HBox(16,PixelAssets.heading(PixelAssets.item(item.getId()),item.getName(),item.getEffect()),spacer,buy);
            row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("card");box.getChildren().add(row);
        }
        box.getChildren().add(text("Equipment must be equipped from Inventory after buying it. Quest items cannot be bought or sold."));
        return box;
    }
}
