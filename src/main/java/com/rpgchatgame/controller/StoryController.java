package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.ui.*;
import com.rpgchatgame.util.UiText;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.*;

/** Choice-driven RPG field/battle view. There is deliberately no custom chat input. */
public final class StoryController {
    private final MainApp app;
    private final GameService game;
    private final TilePane choices=new TilePane(8,8);
    private final GameState state;
    private String category="All";
    public StoryController(MainApp app,GameService game){this.app=app;this.game=game;this.state=game.getCurrentState();}
    private Label text(String value,String style){Label label=new Label(value);label.setWrapText(true);if(!style.isBlank())label.getStyleClass().add(style);return label;}
    private Button button(String title,Runnable action){Button b=new Button(title);b.getStyleClass().addAll("game-button","nav-button");b.setOnAction(e->app.runSafely(action));return b;}
    private ScrollPane scroll(Node content){ScrollPane pane=new ScrollPane(content);pane.setFitToWidth(true);pane.getStyleClass().add("plain-scroll");return pane;}
    public Parent createView(){
        var location=WorldContent.location(state.getCurrentLocation());
        BorderPane root=new BorderPane();root.getStyleClass().addAll("app-root","rpg-root");
        HBox spacer=new HBox();HBox.setHgrow(spacer,Priority.ALWAYS);
        HBox nav=new HBox(10,text(state.getCurrentLocation(),"screen-title"),spacer,button("World",app::showAdventure),button("Inventory",app::showInventory),button("Profile",app::showProfile),button("Home",app::showHome));
        nav.setAlignment(Pos.CENTER_LEFT);nav.setPadding(new Insets(12,16,12,16));nav.getStyleClass().add("top-nav");root.setTop(nav);
        ScrollPane party=scroll(party());party.setPrefWidth(230);party.setMinWidth(205);party.setMaxWidth(250);root.setLeft(party);
        VBox content=new VBox(10,scene(location),narration(),actions());content.setPadding(new Insets(12));root.setCenter(content);
        if(System.getProperty("teleknia.uiCheck.root")==null){var fade=new javafx.animation.FadeTransition(javafx.util.Duration.millis(160),content);fade.setFromValue(.45);fade.setToValue(1);fade.play();}
        return root;
    }
    private VBox party(){
        StatusPanel status=new StatusPanel();status.setCombatMode(true);status.update(state.getCharacter());
        if(!state.getCampaign().getPlayerEffects().isEmpty())status.getChildren().add(3,text(CombatService.effects(state.getCampaign().getPlayerEffects()),"status-badge"));
        if(!state.getCampaign().getEnemyEffects().isEmpty())status.getChildren().add(text("Foe: "+CombatService.effects(state.getCampaign().getEnemyEffects()),"status-badge"));
        var hero=state.getCharacter();
        VBox box=new VBox(10,status,text("Gold: "+hero.getGold()+" / EXP: "+hero.getExp()+" / 100","item-meta"));box.setPadding(new Insets(12,0,12,12));
        var tracked=WorldService.tracked(state);
        VBox quest=new VBox(8,text("TRACKED QUEST","eyebrow"),text(tracked==null?"All milestones complete":tracked.title(),"panel-title"),text(tracked==null?state.getCurrentQuest():tracked.description(),"item-meta"),button("Quest Journal",()->app.showAdventure(1,"")));
        quest.getStyleClass().add("card");box.getChildren().add(quest);
        VBox reputation=new VBox(6,text("REPUTATION","eyebrow"),text("Survivor Camp: "+WorldService.reputation(state,"camp")+" / 10","item-meta"),text("Ember Keepers: "+WorldService.reputation(state,"keepers")+" / 10","item-meta"),button("Skills & Build",()->app.showAdventure(3,"")),button("Codex",()->app.showAdventure(4,"")));
        reputation.getStyleClass().add("card");box.getChildren().add(reputation);return box;
    }
    private VBox scene(WorldContent.Location location){
        VBox card=new VBox(7);card.getStyleClass().addAll("rpg-scene","region-"+location.region().toLowerCase(java.util.Locale.ROOT).replace(' ','-'));
        Label region=text(location.region()+" / "+UiText.tr(state.getCampaign().getDifficulty()),"eyebrow");
        ImageView vista=PixelAssets.view(location.sprite(),2);
        Button map=button("",()->app.showAdventure(0,""));map.setGraphic(vista);map.setTooltip(new Tooltip("Open regional map"));map.getStyleClass().add("scene-vista");map.setId("scene-map");
        VBox hero=new VBox(5,PixelAssets.view("field_"+state.getCharacter().getCharacterClass().toLowerCase(java.util.Locale.ROOT)),text(state.getCharacter().getName(),"actor-name"));hero.setAlignment(Pos.CENTER);hero.setPrefWidth(95);hero.setMaxWidth(115);hero.getChildren().get(1).getProperties().put("literal",true);
        ((Label)hero.getChildren().get(1)).setWrapText(false);((Label)hero.getChildren().get(1)).setMaxWidth(95);
        VBox target=new VBox(5);target.setAlignment(Pos.CENTER);target.setPrefWidth(140);target.setMaxWidth(160);
        var enemy=state.getCampaign().getEnemy();
        if(enemy!=null){
            var data=WorldContent.MONSTERS.get(enemy.getId());
            Button portrait=button("",()->submit("attack"));portrait.setGraphic(PixelAssets.view(data.sprite()));portrait.setId("scene-opponent");portrait.getStyleClass().add("actor-portrait");portrait.setTooltip(new Tooltip("Click to attack"));
            target.getChildren().addAll(portrait,text(enemy.getName(),"actor-name"));
        }else{
            var people=WorldService.npcs(state);
            if(!people.isEmpty()){
                var npc=people.getFirst();Button portrait=button("",()->submit("npc_"+npc.id()));portrait.setGraphic(PixelAssets.view(npc.sprite()));portrait.setId("npc-"+npc.id());portrait.getStyleClass().add("actor-portrait");portrait.setTooltip(new Tooltip("Talk to "+npc.name()));portrait.setDisable(!state.getCampaign().getDialogueNode().isBlank());
                target.getChildren().addAll(portrait,text(npc.name(),"actor-name"),text("Talk / interact","item-meta"));
            }else target.getChildren().addAll(PixelAssets.view("journal"),text("Explore this place","item-meta"));
        }
        for(Node node:target.getChildren())if(node instanceof Label label){label.setWrapText(false);label.setMaxWidth(150);label.setTooltip(new Tooltip(label.getText()));}
        HBox actors=new HBox(12,hero,map,target);actors.setAlignment(Pos.CENTER);HBox.setHgrow(map,Priority.ALWAYS);
        card.getChildren().addAll(region,actors);
        if(enemy!=null){
            ProgressBar health=new ProgressBar((double)enemy.getHp()/enemy.getMaxHp());health.getStyleClass().add("hp-bar");health.setPrefWidth(90);health.setMaxWidth(120);
            Label vitality=text("HP "+enemy.getHp()+" / "+enemy.getMaxHp(),"item-meta");vitality.setId("enemy-vitality");vitality.setWrapText(false);
            Label intent=text("Next: "+UiText.tr(CombatService.intent(state).name()),"intent-label");intent.setTooltip(new Tooltip(CombatService.intent(state).description()));intent.setWrapText(false);
            HBox line=new HBox(10,vitality,health,intent);line.setAlignment(Pos.CENTER);card.getChildren().add(line);

        }
        card.setMinHeight(175);card.setPrefHeight(215);card.setMaxHeight(280);VBox.setVgrow(card,Priority.ALWAYS);
        // Scenery scales only in whole native 64px increments.
        card.heightProperty().addListener((o,b,a)->{double size=64*Math.max(1,Math.min(3,Math.floor((a.doubleValue()-(enemy!=null?80:52))/64)));vista.setFitWidth(size);vista.setFitHeight(size);
            double actorSize=a.doubleValue()>=260?128:64;
            ((ImageView)hero.getChildren().getFirst()).setFitWidth(actorSize);((ImageView)hero.getChildren().getFirst()).setFitHeight(actorSize);
            if(!target.getChildren().isEmpty()&&target.getChildren().getFirst() instanceof Button portrait&&portrait.getGraphic() instanceof ImageView image){image.setFitWidth(actorSize);image.setFitHeight(actorSize);}});
        return card;
    }
    private VBox narration(){
        String speaker=state.getCampaign().getEnemy()!=null?"BATTLE":state.getCampaign().getDialogueNode().isBlank()?"EXPLORATION":WorldContent.NPCS.get(WorldContent.DIALOGUES.get(state.getCampaign().getDialogueNode()).npc()).name();
        Label body=text(state.getCurrentStorySummary(),"narration-text");body.getProperties().put("literal",true);body.setId("scene-narration");
        ScrollPane scroll=scroll(body);scroll.setMinHeight(35);scroll.setPrefHeight(50);scroll.setMaxHeight(75);
        Button read=button("Read scene",this::readScene);read.getStyleClass().add("read-scene-button");
        HBox heading=new HBox(10,text(speaker,"eyebrow"),read);heading.setAlignment(Pos.CENTER_LEFT);
        VBox card=new VBox(5,heading,scroll);card.getStyleClass().add("narration-card");
        if(!state.getStoryLogs().isEmpty()&&state.getStoryLogs().getLast().getSpeaker()==StoryLog.Speaker.SYSTEM){
            Label result=text(state.getStoryLogs().getLast().getMessage(),"outcome-label");result.setWrapText(false);result.setMaxHeight(22);result.setTooltip(new Tooltip(result.getText()));card.getChildren().add(result);
        }
        return card;
    }
    private void readScene(){
        Dialog<Void> dialog=new Dialog<>();dialog.setTitle(UiText.tr("Scene details"));dialog.initOwner(app.getWindow());
        Label body=text(state.getCurrentStorySummary(),"narration-text");body.getProperties().put("literal",true);body.setMaxWidth(600);
        ScrollPane page=scroll(body);page.setPrefSize(620,300);dialog.getDialogPane().setContent(page);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-font-size: "+14*app.getSettings().textScale()+"px;");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType(UiText.tr("Close"),ButtonBar.ButtonData.CANCEL_CLOSE));dialog.showAndWait();
    }
    private VBox actions(){
        choices.getStyleClass().add("choices-box");choices.setPrefColumns(2);choices.prefTileWidthProperty().bind(choices.widthProperty().subtract(8).divide(2));
        HBox filters=new HBox(8);ToggleGroup group=new ToggleGroup();
        for(String name:state.getCampaign().getEnemy()!=null?List.of("All","Skills","Items"):(state.getCampaign().getDialogueNode().isBlank()?List.of("All","Interact","Travel"):List.of("All","Interact"))){
            ToggleButton filter=new ToggleButton(name);filter.getStyleClass().addAll("game-button","action-filter");filter.setToggleGroup(group);filter.setSelected(name.equals("All"));
            filter.setOnAction(e->{if(!filter.isSelected())filter.setSelected(true);category=name;populate();app.refreshUi();});filters.getChildren().add(filter);
        }
        ScrollPane list=scroll(choices);list.setMinHeight(120);list.setPrefHeight(180);list.setMaxHeight(235);
        VBox box=new VBox(8,filters,list);VBox.setVgrow(list,Priority.SOMETIMES);box.getStyleClass().add("rpg-actions");populate();return box;
    }
    private void populate(){
        choices.getChildren().clear();
        for(var choice:state.getCurrentChoices()){
            String id=choice.getAction();boolean travel=id.startsWith("go_");boolean item=id.equals("potion")||id.equals("mana");
            if(category.equals("Travel")&&!travel||category.equals("Interact")&&travel||category.equals("Items")&&!item||category.equals("Skills")&&(item||id.equals("flee")))continue;
            Button action=button(choice.getLabel(),()->submit(id));action.getStyleClass().add("choice-button");action.setMaxWidth(Double.MAX_VALUE);
            String tip="";if(state.getCampaign().getEnemy()!=null){tip=UiText.tr(CombatService.unavailable(state,id));action.setDisable(!tip.isBlank());var skill=SkillCatalog.find(state,id);if(skill!=null)tip+=(tip.isBlank()?"":"\n")+UiText.tr(skill.description());}
            action.setTooltip(new Tooltip(tip.isBlank()?UiText.tr("Choose this action"):tip));choices.getChildren().add(action);
        }
    }
    private void submit(String action){
        int level=state.getCharacter().getLevel(),known=state.getCampaign().getCodex().size();
        long completed=WorldContent.QUESTS.keySet().stream().filter(state.getCampaign()::has).count();
        int potions=state.getInventory().stream().filter(i->i.getItem().getType()==Item.ItemType.CONSUMABLE).mapToInt(InventoryItem::getQuantity).sum();
        var response=game.performAction(action);var next=game.getCurrentState();
        int after=next.getInventory().stream().filter(i->i.getItem().getType()==Item.ItemType.CONSUMABLE).mapToInt(InventoryItem::getQuantity).sum();
        AudioService.Sound sound=next.getCharacter().getLevel()>level?AudioService.Sound.LEVEL_UP:
                WorldContent.QUESTS.keySet().stream().filter(next.getCampaign()::has).count()>completed?AudioService.Sound.QUEST_COMPLETE:
                after<potions?AudioService.Sound.POTION:response.getHpChange()<0?AudioService.Sound.DAMAGE:
                next.getCampaign().getCodex().size()>known?AudioService.Sound.DISCOVERY:response.getGoldChange()>0||!response.getItemsGained().isEmpty()?AudioService.Sound.REWARD:AudioService.Sound.ACTION;
        app.getAudio().play(sound);app.showStory();
    }
}
