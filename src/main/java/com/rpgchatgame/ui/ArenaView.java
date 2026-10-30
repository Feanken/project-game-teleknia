package com.rpgchatgame.ui;
import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.util.UiText;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import static com.rpgchatgame.ui.RpgUi.*;

/** Preparation screen: browse trials without entering; action remains visible below details. */
public final class ArenaView extends BorderPane {
    private final MainApp app;private final GameService game;private final BorderPane details=new BorderPane();
    private final java.util.Map<String,Button> entries=new java.util.LinkedHashMap<>();
    public ArenaView(MainApp app,GameService game){
        this.app=app;this.game=game;setMinHeight(0);setPadding(new Insets(12));getStyleClass().add("collection-view");setId("arena-view");
        VBox list=new VBox(8,title("Arena"));list.setPrefWidth(146);list.setMinWidth(132);list.setMaxWidth(160);
        for(var c:BondService.CHALLENGES){Button b=button(app,"",()->select(c));b.setId("entry-"+c.id());b.setMinHeight(104);b.setPrefHeight(104);b.setMaxHeight(104);b.setMaxWidth(Double.MAX_VALUE);b.getStyleClass().add("arena-list-entry");var icon=PixelAssets.view(c.sprite());icon.setFitWidth(32);icon.setFitHeight(32);VBox names=new VBox(4,text(c.name()),text(game.getCurrentState().getCampaign().has("challenge_"+c.id())?"Cleared":"First clear"));names.setMinWidth(0);names.setPrefHeight(72);names.setMaxHeight(72);for(var node:names.getChildren())if(node instanceof Label label){label.setMinHeight(0);label.setPrefHeight(32);label.setMaxHeight(32);}HBox.setHgrow(names,Priority.ALWAYS);HBox tile=new HBox(8,icon,names);tile.setAlignment(Pos.CENTER_LEFT);tile.setMinWidth(0);b.setGraphic(tile);b.setAccessibleText(c.name());list.getChildren().add(b);entries.put(c.id(),b);}
        ScrollPane nav=scroll(list);nav.setPrefWidth(154);nav.setMinWidth(132);setLeft(nav);details.getStyleClass().add("arena-details");setCenter(details);BorderPane.setMargin(details,new Insets(0,0,0,12));
        String id=app.recall("arena.selected","ash");select(BondService.CHALLENGES.stream().filter(c->c.id().equals(id)).findFirst().orElse(BondService.CHALLENGES.getFirst()));
    }
    private VBox metric(String label,int value){VBox box=new VBox(3,text(label),title(Integer.toString(value)));box.getStyleClass().add("arena-metric");HBox.setHgrow(box,Priority.ALWAYS);return box;}
    private void select(BondService.Challenge c){
        app.remember("arena.selected",c.id());entries.forEach((key,b)->{b.getStyleClass().remove("collection-selected");if(key.equals(c.id()))b.getStyleClass().add("collection-selected");});
        var s=game.getCurrentState();boolean cleared=s.getCampaign().has("challenge_"+c.id());
        Label heading=title(c.name());Label state=text(cleared?"CLEARED / PRACTICE":"FIRST CLEAR AVAILABLE");state.getStyleClass().add("arena-badge");
        HBox numbers=new HBox(8,metric("HP",c.hp()),metric("Attack",c.attack()),metric("Defense",c.defense()));
        HBox titleLine=new HBox(10,heading,state);titleLine.setAlignment(Pos.CENTER_LEFT);VBox stats=new VBox(10,titleLine,numbers,text(c.modifier().replace("; 10 gold entry.","")));stats.setMinWidth(0);HBox.setHgrow(stats,Priority.ALWAYS);
        StackPane portrait=new StackPane(PixelAssets.view(c.sprite(),2));portrait.getStyleClass().add("arena-portrait");
        HBox preview=new HBox(14,portrait,stats);preview.setAlignment(Pos.CENTER_LEFT);
        VBox top=new VBox(8,preview);top.setPadding(new Insets(14));details.setTop(top);
        String material=ArenaReadiness.material(c.id());
        HBox reward=new HBox(12,PixelAssets.view(PixelAssets.item(material)),new VBox(5,title(cleared?"First-clear reward claimed":"First-clear reward"),text(ItemCatalog.equippedName(material)+" x2"),text("Replay: practice only, no rewards.")));reward.setAlignment(Pos.CENTER_LEFT);reward.getStyleClass().add("arena-block");
        var hero=s.getCharacter();var ally=BondService.companion(s.getCampaign().getCompanion());
        VBox prep=new VBox(7,title("Your preparation"),text(hero.getCharacterClass()+" / Lv "+hero.getLevel()+" / HP "+hero.getHp()+" / "+hero.getMaxHp()+" / MP "+hero.getMp()+" / "+hero.getMaxMp()),text(UiText.tr("Companion")+": "+(ally==null?UiText.tr("None"):ally.name())),text("Guard against heavy attacks. Support and items still spend a turn."));prep.getStyleClass().add("arena-block");
        HBox manage=new HBox(8,button(app,"Build",()->app.showAdventure(3,"")),button(app,"Inventory",app::showInventory));prep.getChildren().add(manage);
        VBox conditions=new VBox(6,title("Entry requirements"));conditions.getStyleClass().add("arena-block");
        for(var requirement:ArenaReadiness.requirements(s)){Label line=text((requirement.met()?"✓  ":"○  ")+UiText.tr(requirement.label()));line.getStyleClass().add(requirement.met()?"requirement-met":"requirement-missing");conditions.getChildren().add(line);}
        VBox body=new VBox(10,reward,prep,conditions,text("Entry is not refunded. Retreat and defeat keep their normal penalties. Story ending and unique loot are preserved."));body.setPadding(new Insets(0,14,10,14));ScrollPane page=scroll(body);page.setMinHeight(0);page.setId("arena-detail-scroll");details.setCenter(page);
        Label cost=text(UiText.format("Entry: {0} gold / You have: {1}",10,hero.getGold()));
        Label reason=text(ArenaReadiness.ready(s)?"Ready to challenge":UiText.tr("Next step")+": "+UiText.tr(ArenaReadiness.blocked(s)));reason.setId("arena-blocked-reason");reason.getStyleClass().add(ArenaReadiness.ready(s)?"requirement-met":"requirement-missing");
        Button enter=button(app,"Enter arena",()->{if(!ArenaReadiness.ready(game.getCurrentState())){select(c);return;}String message=game.challenge(c.id());app.showStory();app.announce(message);});enter.setId("challenge-"+c.id());enter.setDisable(!ArenaReadiness.ready(s));enter.setTooltip(tip(reason.getText()));enter.setMaxWidth(Double.MAX_VALUE);
        VBox action=new VBox(6,cost,reason,enter);action.getStyleClass().add("arena-action");details.setBottom(action);
    }
}
