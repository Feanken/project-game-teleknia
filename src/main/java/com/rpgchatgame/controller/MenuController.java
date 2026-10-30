package com.rpgchatgame.controller;
import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.*;
import com.rpgchatgame.ui.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import static com.rpgchatgame.ui.RpgUi.*;

/** Grouped pause menu with a next-objective reminder, never an implicit new game. */
public final class MenuController {
    private final MainApp app;private final GameService game;
    public MenuController(MainApp app,GameService game){this.app=app;this.game=game;}
    private void group(VBox list,String title){Label heading=text(title);heading.getStyleClass().add("menu-group-title");list.getChildren().add(heading);}
    private Button entry(VBox list,String label,String id,Runnable action,boolean enabled){Button b=button(app,label,action);b.setId(id);b.setMaxWidth(Double.MAX_VALUE);b.setDisable(!enabled);b.getStyleClass().add("menu-option");list.getChildren().add(b);return b;}
    public Parent createView(){
        boolean playable=game.hasCurrentGame()||game.hasSavedGame();BorderPane root=new BorderPane();root.getStyleClass().addAll("app-root","menu-root");
        HBox heading=new HBox(20,title("Menu"),button(app,"Back",app::back));heading.setPadding(new Insets(16,24,12,24));root.setTop(heading);
        VBox list=new VBox(3);list.setPadding(new Insets(4,24,20,24));
        String[] sections={"WORLD MAP","QUEST JOURNAL","CAMP MARKET","SKILLS & BUILD","ARCHIVE","CHRONICLE","CAMP & BONDS","CHALLENGES","GALLERY"};
        group(list,"ADVENTURE");for(int i:new int[]{0,1,4,5,7,8}){final int n=i;entry(list,sections[i],"menu-section-"+i,()->app.showAdventure(n,""),playable);}
        group(list,"CHARACTER");entry(list,"Inventory","menu-inventory",app::showInventory,playable);for(int i:new int[]{3,6,2}){final int n=i;entry(list,sections[i],"menu-section-"+i,()->app.showAdventure(n,""),playable);}entry(list,"Profile","menu-profile",app::showProfile,playable);
        group(list,"SYSTEM");entry(list,"Saves","menu-saves",()->app.showSaves(false,""),true);entry(list,"Settings","menu-settings",app::showSettings,true);entry(list,"Help","menu-help",app::showHelp,true);entry(list,"Home","menu-home",app::showHome,true);
        var navigation=scroll(list);navigation.setPrefWidth(310);root.setLeft(navigation);
        VBox party=new VBox(18);party.setPadding(new Insets(20));
        if(game.hasCurrentGame()){
            var state=game.getCurrentState();var hero=state.getCharacter();StatusPanel stats=new StatusPanel();stats.update(hero);stats.setPortraitVisible(false);stats.setMinWidth(0);
            HBox summary=new HBox(24,PixelAssets.view("field_"+hero.getCharacterClass().toLowerCase(java.util.Locale.ROOT),2),stats);summary.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(stats,Priority.ALWAYS);party.getChildren().add(summary);
            var path=MasteryService.active(state);
            if(path!=null)party.getChildren().add(text("Specialization: "+path.name()));
            else if(hero.getLevel()>=5&&state.getCampaign().has("guardian"))party.getChildren().add(button(app,"Choose your specialization",()->{app.remember("codex.3.category","Specialization");app.showAdventure(3,"");}));
            var quest=WorldService.tracked(state);VBox goal=new VBox(8,title("Next on your journey"),text(state.getCurrentLocation()),text(quest==null?state.getCurrentQuest():quest.title()),text(quest==null?"Explore the borderlands or prepare for the arena.":LivingService.questNext(state,quest.id())),button(app,"Open Quest Journal",()->app.showAdventure(1,"")));goal.getStyleClass().add("journey-reminder");party.getChildren().add(goal);
        }else party.getChildren().addAll(title("Teleknia"),text("Continue your journey or begin a new story from Home."));
        root.setCenter(scroll(party));return root;
    }
}
