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
    private String animation="";
    private DialoguePager reader;
    public StoryController(MainApp app,GameService game){this.app=app;this.game=game;this.state=game.getCurrentState();}
    public StoryController(MainApp app,GameService game,String animation){this(app,game);this.animation=animation;}
    private Label text(String value,String style){Label label=new Label(value);label.setWrapText(true);if(!style.isBlank())label.getStyleClass().add(style);return label;}
    private Button button(String title,Runnable action){Button b=new Button(title);b.getStyleClass().addAll("game-button","nav-button");b.setOnAction(e->app.runSafely(action));return b;}
    private ScrollPane scroll(Node content){ScrollPane pane=new ScrollPane(content);pane.setFitToWidth(true);pane.getStyleClass().add("plain-scroll");return pane;}
    public Parent createView(){
        var location=WorldContent.location(state.getCurrentLocation());
        BorderPane root=new BorderPane();root.getStyleClass().addAll("app-root","rpg-root","parchment-shell");
        HBox spacer=new HBox();HBox.setHgrow(spacer,Priority.ALWAYS);
        root.setTop(RpgUi.nav(app,state.getCurrentLocation()));
        VBox actionPanel=actions();actionPanel.setMinWidth(0);HBox.setHgrow(actionPanel,Priority.ALWAYS);
        HBox lower=new HBox(12,actionPanel,minimap(location));lower.setMinHeight(150);
        VBox stage=scene(location);stage.setMaxHeight(Double.MAX_VALUE);stage.setMinWidth(0);
        VBox dialogue=narration();dialogue.setMinWidth(250);dialogue.setPrefWidth(330);dialogue.setMaxWidth(410);
        HBox upper=new HBox(14,stage,dialogue);HBox.setHgrow(stage,Priority.ALWAYS);VBox.setVgrow(upper,Priority.ALWAYS);upper.setMinHeight(230);
        var hero=state.getCharacter();
        HBox stats=new HBox(22,text(hero.getName(),"party-name"),text("Lv "+hero.getLevel()+"  "+hero.getCharacterClass(),"stat-value"),text("HP "+hero.getHp()+" / "+hero.getMaxHp(),"hp-value"),text("MP "+hero.getMp()+" / "+hero.getMaxMp(),"mp-value"));stats.getChildren().getFirst().getProperties().put("literal",true);stats.getStyleClass().add("party-strip");stats.setAlignment(Pos.CENTER_LEFT);
        VBox content=new VBox(12,stats,upper,lower);content.setPadding(new Insets(16));root.setCenter(content);
        if(!app.getSettings().reducedMotion()&&System.getProperty("teleknia.uiCheck.root")==null){var fade=new javafx.animation.FadeTransition(javafx.util.Duration.millis(160),content);fade.setFromValue(.45);fade.setToValue(1);fade.play();}
        if(!app.getSettings().reducedMotion()&&System.getProperty("teleknia.uiCheck.root")==null&&!animation.isBlank()){
            Node actor=content.lookup("#hero-actor"),target=content.lookup("#target-actor");
            if(actor!=null){
                boolean heal=animation.equals("heal");
                if(heal)actor.setEffect(new javafx.scene.effect.DropShadow(12,javafx.scene.paint.Color.LIGHTGREEN));
                var pulse=new javafx.animation.FadeTransition(javafx.util.Duration.millis(120),actor);pulse.setFromValue(1);pulse.setToValue(heal?.65:.4);pulse.setCycleCount(2);pulse.setAutoReverse(true);pulse.setOnFinished(e->actor.setEffect(null));
                if(heal)pulse.play();else{
                    var attack=new javafx.animation.TranslateTransition(javafx.util.Duration.millis(110),actor);attack.setByX(animation.equals("hit")?-8:8);attack.setCycleCount(2);attack.setAutoReverse(true);
                    new javafx.animation.SequentialTransition(attack,pulse).play();
                }
                if(target!=null&&!state.getCampaign().getEnemyEffects().isEmpty()){
                    var status=new javafx.animation.FadeTransition(javafx.util.Duration.millis(160),target);status.setFromValue(1);status.setToValue(.45);status.setCycleCount(2);status.setAutoReverse(true);status.play();
                }
            }
        }
        return root;
    }
    private Button minimap(WorldContent.Location location){
        ImageView art=PixelAssets.view(location.sprite(),2);art.setEffect(new javafx.scene.effect.ColorAdjust(0,-.65,-.15,-.2));
        Label label=text("MAP","minimap-label");label.setMouseTransparent(true);StackPane graphic=new StackPane(art,label);graphic.setPrefSize(138,128);
        Button b=button("",app::showMap);b.setId("scene-map");b.setGraphic(graphic);b.getStyleClass().add("minimap-button");b.setPrefWidth(158);b.setMinWidth(150);b.setMaxWidth(176);b.setTooltip(new Tooltip(UiText.tr("Open regional map")));b.setAccessibleText(UiText.tr("Open regional map"));return b;
    }
    private VBox party(){
        StatusPanel status=new StatusPanel();status.setCombatMode(true);status.update(state.getCharacter());
        if(!state.getCampaign().getPlayerEffects().isEmpty())status.getChildren().add(3,text(CombatService.effects(state.getCampaign().getPlayerEffects()),"status-badge"));
        if(!state.getCampaign().getEnemyEffects().isEmpty())status.getChildren().add(text("Foe: "+CombatService.effects(state.getCampaign().getEnemyEffects()),"status-badge"));
        var hero=state.getCharacter();
        VBox box=new VBox(10,status,text("Gold: "+hero.getGold()+" / EXP: "+hero.getExp()+" / 100","item-meta"));box.setPadding(new Insets(12,0,12,12));box.getStyleClass().add("party-panel");
        var ally=BondService.companion(state.getCampaign().getCompanion());
        if(ally!=null)box.getChildren().add(PixelAssets.heading(ally.sprite(),ally.name(),ally.role()));
        box.getChildren().add(button(ally==null?"Camp & Bonds":ally.name()+" / Affinity "+BondService.affinity(state,ally),()->app.showAdventure(6,"")));
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
        StackPane map=new StackPane(vista);map.getStyleClass().add("scene-vista");map.setId("scene-vista");
        VBox hero=new VBox(5,PixelAssets.view("field_"+state.getCharacter().getCharacterClass().toLowerCase(java.util.Locale.ROOT)),text(state.getCharacter().getName(),"actor-name"));hero.setId("hero-actor");hero.setAlignment(Pos.CENTER);hero.setPrefWidth(95);hero.setMaxWidth(115);hero.getChildren().get(1).getProperties().put("literal",true);
        ((Label)hero.getChildren().get(1)).setWrapText(false);((Label)hero.getChildren().get(1)).setMaxWidth(95);
        VBox target=new VBox(5);target.setId("target-actor");target.setAlignment(Pos.CENTER);target.setPrefWidth(140);target.setMaxWidth(160);
        var enemy=state.getCampaign().getEnemy();
        if(enemy!=null){
            var data=WorldContent.MONSTERS.get(enemy.getId());
            Button portrait=button("",()->submit("attack"));portrait.setGraphic(PixelAssets.view(data==null?BondService.CHALLENGES.stream().filter(c->enemy.getId().equals("arena_"+c.id())).findFirst().map(BondService.Challenge::sprite).orElse("field_guardian"):data.sprite()));portrait.setId("scene-opponent");portrait.getStyleClass().add("actor-portrait");portrait.setTooltip(new Tooltip("Click to attack"));
            target.getChildren().addAll(portrait,text(enemy.getName(),"actor-name"));
        }else{
            var people=WorldService.npcs(state);
            if(!people.isEmpty()){
                var npc=people.getFirst();Button portrait=button("",()->submit("npc_"+npc.id()));portrait.setGraphic(PixelAssets.view(npc.sprite()));portrait.setId("npc-"+npc.id());portrait.getStyleClass().add("actor-portrait");portrait.setTooltip(new Tooltip("Talk to "+npc.name()));portrait.setDisable(!state.getCampaign().getDialogueNode().isBlank());
                target.getChildren().addAll(portrait,text(npc.name(),"actor-name"));
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
        card.setMinHeight(enemy!=null?175:150);card.setPrefHeight(215);card.setMaxHeight(280);VBox.setVgrow(card,Priority.ALWAYS);
        // Scenery scales only in whole native 64px increments.
        card.heightProperty().addListener((o,b,a)->{double size=64*Math.max(1,Math.min(3,Math.floor((a.doubleValue()-(enemy!=null?80:52))/64)));vista.setFitWidth(size);vista.setFitHeight(size);
            double actorSize=a.doubleValue()>=260?128:64;
            ((ImageView)hero.getChildren().getFirst()).setFitWidth(actorSize);((ImageView)hero.getChildren().getFirst()).setFitHeight(actorSize);
            if(!target.getChildren().isEmpty()&&target.getChildren().getFirst() instanceof Button portrait&&portrait.getGraphic() instanceof ImageView image){image.setFitWidth(actorSize);image.setFitHeight(actorSize);}});
        return card;
    }
    private VBox narration(){
        String speaker=state.getCampaign().getEnemy()!=null?"BATTLE":state.getCampaign().getDialogueNode().isBlank()?"EXPLORATION":WorldContent.NPCS.get(WorldContent.DIALOGUES.get(state.getCampaign().getDialogueNode()).npc()).name();
        reader=new DialoguePager(state.getCurrentStorySummary(),app.getSettings().reducedMotion()||System.getProperty("teleknia.uiCheck.root")!=null);
        choices.disableProperty().bind(reader.finishedProperty().not());
        ScrollPane scroll=scroll(reader);scroll.setId("narration-scroll");scroll.setMinHeight(100);scroll.setPrefHeight(280);scroll.setMaxHeight(Double.MAX_VALUE);VBox.setVgrow(scroll,Priority.ALWAYS);
        Button read=button("Read scene",this::readScene);read.getStyleClass().add("read-scene-button");
        HBox heading=new HBox(10,text(speaker,"eyebrow"),read);heading.setAlignment(Pos.CENTER_LEFT);
        VBox card=new VBox(12,heading,scroll);card.getStyleClass().add("narration-card");
        scroll.setOnMouseClicked(e->{if(e.getButton()==javafx.scene.input.MouseButton.PRIMARY){reader.requestFocus();reader.advance();e.consume();}});
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
        for(String name:state.getCampaign().getEnemy()!=null?List.of("All","Skills","Items"):(state.getCampaign().getDialogueNode().isBlank()?List.of("All","Interact"):List.of("All","Interact"))){
            ToggleButton filter=new ToggleButton(name);filter.getStyleClass().addAll("game-button","action-filter");filter.setToggleGroup(group);filter.setSelected(name.equals("All"));
            filter.setOnAction(e->{if(!filter.isSelected())filter.setSelected(true);category=name;populate();app.refreshUi();});filters.getChildren().add(filter);
        }
        ScrollPane list=scroll(choices);list.setMinHeight(100);list.setPrefHeight(180);list.setMaxHeight(235);
        VBox box=new VBox(8,filters,list);VBox.setVgrow(list,Priority.SOMETIMES);box.getStyleClass().add("rpg-actions");populate();return box;
    }
    private void populate(){
        choices.getChildren().clear();
        for(var choice:state.getCurrentChoices()){
            String id=choice.getAction();if(id.startsWith("go_"))continue;boolean travel=false;boolean item=Set.of("potion","mana","greater","focus").contains(id);
            if(category.equals("Travel")&&!travel||category.equals("Interact")&&travel||category.equals("Items")&&!item||category.equals("Skills")&&(item||id.equals("flee")))continue;
            Button action=button(choice.getLabel(),()->submit(id));action.getStyleClass().add("choice-button");action.setMaxWidth(Double.MAX_VALUE);
            String tip="";if(state.getCampaign().getEnemy()!=null){tip=UiText.tr(CombatService.unavailable(state,id));action.setDisable(!tip.isBlank());var skill=SkillCatalog.find(state,id);if(skill!=null)tip+=(tip.isBlank()?"":"\n")+UiText.tr(skill.description());}
            action.setTooltip(new Tooltip(tip.isBlank()?UiText.tr("Choose this action"):tip));choices.getChildren().add(action);
        }
    }
    private void submit(String action){
        if(choices.isDisabled())return;
        int level=state.getCharacter().getLevel(),known=state.getCampaign().getCodex().size();
        long completed=WorldContent.QUESTS.keySet().stream().filter(state.getCampaign()::has).count();
        int potions=state.getInventory().stream().filter(i->i.getItem().getType()==Item.ItemType.CONSUMABLE).mapToInt(InventoryItem::getQuantity).sum();
        var response=game.performAction(action);var next=game.getCurrentState();
        int after=next.getInventory().stream().filter(i->i.getItem().getType()==Item.ItemType.CONSUMABLE).mapToInt(InventoryItem::getQuantity).sum();
        AudioService.Sound sound=next.getCharacter().getLevel()>level?AudioService.Sound.LEVEL_UP:
                WorldContent.QUESTS.keySet().stream().filter(next.getCampaign()::has).count()>completed?AudioService.Sound.QUEST_COMPLETE:
                after<potions?AudioService.Sound.POTION:response.getHpChange()<0?AudioService.Sound.DAMAGE:
                next.getCampaign().getCodex().size()>known?AudioService.Sound.DISCOVERY:response.getGoldChange()>0||!response.getItemsGained().isEmpty()?AudioService.Sound.REWARD:AudioService.Sound.ACTION;
        app.getAudio().play(sound);app.showStory(response.getHpChange()>0?"heal":state.getCampaign().getEnemy()!=null||next.getCampaign().getEnemy()!=null?(response.getHpChange()<0?"hit":"attack"):"");
    }
}
