package com.rpgchatgame;

import com.rpgchatgame.controller.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.repository.SaveGameRepository;
import com.rpgchatgame.ui.*;
import com.rpgchatgame.util.*;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

public class MainApp extends Application {
    private Stage primaryStage;
    private Scene scene;
    private GameService gameService;
    private AudioService audio;
    private UiSettings settings;
    private BorderPane shell;
    private boolean returning;
    private String screen="HOME";
    private final java.util.Deque<String> history=new java.util.ArrayDeque<>();

    @Override public void start(Stage stage) {
        primaryStage=stage;gameService=new GameService();settings=new UiSettings();UiText.setLanguage(settings.language());
        audio=new AudioService();shell=new BorderPane();scene=new Scene(shell,1180,820);
        scene.getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        scene.addEventFilter(ActionEvent.ACTION,event->{if(event.getTarget() instanceof ButtonBase button&&!button.getStyleClass().contains("toggle-button"))audio.play(AudioService.Sound.CLICK);});
        scene.addEventFilter(KeyEvent.KEY_PRESSED,this::shortcut);
        primaryStage.setTitle(Constants.APP_TITLE);primaryStage.setMinWidth(980);primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);primaryStage.setWidth(settings.width());primaryStage.setHeight(settings.height());
        showHome();primaryStage.show();applyPreferences();
    }
    private void shortcut(KeyEvent event){
        if(event.isAltDown()||event.isControlDown()||event.isMetaDown())return;
        switch(event.getCode()){
            case F1->runSafely(this::showHelp);
            case F5->runSafely(()->showSaves(true,""));
            case F9->runSafely(()->showSaves(false,""));
            case F11->runSafely(()->setFullscreen(!primaryStage.isFullScreen()));
            case ESCAPE->runSafely(()->{if(primaryStage.isFullScreen())setFullscreen(false);else if(screen.equals("SETTINGS")||screen.equals("HELP")||screen.equals("SAVES"))back();else if(screen.equals("STORY")&&!gameService.getCurrentState().getCampaign().getDialogueNode().isBlank()){gameService.performAction("dialogue_close");showStory();}else showSettings();});
            default->{
                if(!screen.equals("STORY")||event.isShiftDown()||scene.getFocusOwner() instanceof TextInputControl||scene.getFocusOwner() instanceof ComboBoxBase)return;
                int index=switch(event.getCode()){case DIGIT1,NUMPAD1->0;case DIGIT2,NUMPAD2->1;case DIGIT3,NUMPAD3->2;case DIGIT4,NUMPAD4->3;case DIGIT5,NUMPAD5->4;case DIGIT6,NUMPAD6->5;case DIGIT7,NUMPAD7->6;case DIGIT8,NUMPAD8->7;case DIGIT9,NUMPAD9->8;case DIGIT0,NUMPAD0->9;default->-1;};
                if(index<0)return;
                var pane=scene.lookup(".choices-box");if(pane instanceof Parent parent&&index<parent.getChildrenUnmodifiable().size()&&parent.getChildrenUnmodifiable().get(index) instanceof Button button)button.fire();
            }
        }
        event.consume();
    }
    private Button utility(String label,String tooltip,Runnable action){
        Button button=new Button(label);button.getStyleClass().addAll("game-button","nav-button");button.setTooltip(new Tooltip(tooltip));button.setOnAction(e->runSafely(action));return button;
    }
    private void footer(){
        HBox tools=new HBox(8,utility("Saves","F5 Save slots / F9 Load slots",()->showSaves(false,"")),utility("Help","F1",this::showHelp),utility("Settings","Esc",this::showSettings));
        if(screen.equals("SETTINGS")){tools.getStyleClass().add("audio-controls");shell.setBottom(tools);}
        else {AudioControls controls=new AudioControls(audio);controls.getChildren().add(2,tools);shell.setBottom(controls);}
    }
    private void setView(String id,Parent parent){
        if(!returning&&!java.util.Set.of("SAVES","HELP","SETTINGS").contains(id))history.clear();
        screen=id;shell.setCenter(parent);footer();refreshUi();
    }
    private void overlay(String id,Parent parent){
        if(!returning&&!screen.equals(id))history.push(screen);
        setView(id,parent);
    }
    public void refreshUi(){
        if(scene==null)return;
        shell.setStyle("-fx-font-size: "+14*settings.textScale()+"px;");UiLocalization.apply(shell);
    }
    public void applyPreferences(){UiText.setLanguage(settings.language());primaryStage.setFullScreen(settings.fullscreen());refreshUi();}
    public void setFullscreen(boolean value){settings.setFullscreen(value);primaryStage.setFullScreen(value);}
    public void showHome(){setView("HOME",new HomeController(this,gameService).createView());audio.playMusic(true);}
    public void showStory(){setView("STORY",new StoryController(this,gameService).createView());audio.playScene(WorldContent.location(gameService.getCurrentState().getCurrentLocation()).region(),gameService.getCurrentState().getCampaign().getEnemy()==null?null:gameService.getCurrentState().getCampaign().getEnemy().getId());}
    public void showInventory(){setView("INVENTORY",new InventoryController(this,gameService).createView());}
    public void showProfile(){setView("PROFILE",new ProfileController(this,gameService).createView());}
    public void showAdventure(){showAdventure(0,"");}
    public void showAdventure(int tab,String message){
        Parent view=new AdventureController(this,gameService,tab).createView();setView("ADVENTURE:"+tab,view);
        if(!message.isBlank())javafx.application.Platform.runLater(()->{var label=view.lookup(".feedback-label");if(label instanceof Label feedback){feedback.setText(message);refreshUi();}});
    }
    public void showSaves(boolean saving,String message){overlay("SAVES",new SavesController(this,gameService,saving,message).createView());}
    public void showSettings(){overlay("SETTINGS",new SettingsController(this).createView());}
    public void showHelp(){showTutorial(0);}
    public void showTutorial(int step){overlay("HELP",new TutorialController(this,step).createView());}
    public void back(){String target=history.isEmpty()?"HOME":history.pop();returning=true;try{navigate(target);}finally{returning=false;}}
    private void navigate(String target){
        if(target.startsWith("ADVENTURE:")){showAdventure(Integer.parseInt(target.substring(10)),"");return;}
        switch(target){case "STORY"->showStory();case "INVENTORY"->showInventory();case "PROFILE"->showProfile();case "SETTINGS"->showSettings();case "HELP"->showTutorial(0);case "SAVES"->showSaves(false,"");default->showHome();}
    }
    public boolean confirm(String title,String message){
        Alert dialog=new Alert(Alert.AlertType.CONFIRMATION);dialog.initOwner(primaryStage);dialog.setTitle(UiText.tr(title));dialog.setHeaderText(null);dialog.setContentText(UiText.tr(message));
        ButtonType yes=new ButtonType(UiText.tr("Confirm"),ButtonBar.ButtonData.OK_DONE),cancel=new ButtonType(UiText.tr("Cancel"),ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(yes,cancel);dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        return dialog.showAndWait().filter(yes::equals).isPresent();
    }
    public void runSafely(Runnable action){
        try{action.run();}catch(RuntimeException error){
            System.getLogger(MainApp.class.getName()).log(System.Logger.Level.ERROR,"Game action failed",error);
            Alert alert=new Alert(Alert.AlertType.ERROR);alert.initOwner(primaryStage);alert.setTitle(UiText.tr("Unable to complete action"));
            alert.setHeaderText(UiText.tr("No changes were applied to the active game."));alert.setContentText(UiText.tr(error.getMessage()));
            if(error instanceof SaveGameRepository.SaveProblem){
                ButtonType saves=new ButtonType(UiText.tr("Saves")),close=new ButtonType(UiText.tr("Close"),ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(saves,close);if(alert.showAndWait().filter(saves::equals).isPresent())showSaves(false,"");
            }else alert.showAndWait();
        }
    }
    public Stage getWindow(){return primaryStage;}
    public AudioService getAudio(){return audio;}
    public UiSettings getSettings(){return settings;}
    @Override public void stop(){
        if(audio!=null)audio.close();
        if(settings!=null&&primaryStage!=null&&!primaryStage.isFullScreen()){
            try{settings.setWindowSize(primaryStage.getWidth(),primaryStage.getHeight());}catch(RuntimeException error){System.getLogger(MainApp.class.getName()).log(System.Logger.Level.WARNING,"Window preferences not saved",error);}
        }
    }
    public static void main(String[] args){launch(args);}
}
