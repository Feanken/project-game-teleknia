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
    private javafx.scene.image.ImageView backdrop;
    private boolean returning;
    private boolean skipGesture;
    private String menuReturn="HOME";
    private long seenSaveRevision;
    private String saveMessage="";
    private Label saveIndicator,toast;
    private javafx.animation.PauseTransition toastTimer,saveTimer;
    private long lastToastAt;
    private String lastToast="";

    private final java.util.Map<String,String> uiMemory=new java.util.HashMap<>();
    public String recall(String key,String fallback){return uiMemory.getOrDefault(key,fallback);}
    public void remember(String key,String value){uiMemory.put(key,value);}
    public void showMap(){
        Dialog<Void> dialog=new Dialog<>();dialog.initOwner(primaryStage);dialog.setTitle(UiText.tr("WORLD MAP"));
        var map=new AtlasView(this,gameService,()->{dialog.close();showStory();});
        dialog.getDialogPane().setContent(map);dialog.getDialogPane().getButtonTypes().add(new ButtonType(UiText.tr("Close"),ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-font-size: "+14*settings.textScale()+"px;");
        dialog.getDialogPane().setPrefSize(Math.min(940,primaryStage.getWidth()-40),Math.min(600,primaryStage.getHeight()-80));
        dialog.setOnShown(e->map.unfold());dialog.showAndWait();
    }
    private String screen="HOME";
    private final java.util.Deque<String> history=new java.util.ArrayDeque<>();

    @Override public void start(Stage stage) {
        primaryStage=stage;gameService=new GameService();settings=new UiSettings();UiText.setLanguage(settings.language());
        audio=new AudioService();shell=new BorderPane();
        backdrop=new javafx.scene.image.ImageView();backdrop.setMouseTransparent(true);backdrop.setSmooth(true);
        var tint=new javafx.scene.effect.ColorAdjust(0,-.65,-.1,-.35);var blur=new javafx.scene.effect.GaussianBlur(9);blur.setInput(tint);backdrop.setEffect(blur);
        toast=new Label();toast.setId("action-toast");toast.getStyleClass().add("action-toast");toast.setWrapText(true);toast.setMaxWidth(380);toast.setVisible(false);toast.setManaged(false);toast.setMouseTransparent(true);
        StackPane layers=new StackPane(backdrop,shell,toast);StackPane.setAlignment(toast,javafx.geometry.Pos.TOP_RIGHT);StackPane.setMargin(toast,new javafx.geometry.Insets(86,24,0,24));scene=new Scene(layers,1180,820);backdrop.fitWidthProperty().bind(layers.widthProperty());backdrop.fitHeightProperty().bind(layers.heightProperty());
        scene.getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        scene.addEventFilter(ActionEvent.ACTION,event->{if(event.getTarget() instanceof ButtonBase button&&!button.getStyleClass().contains("toggle-button"))audio.play(AudioService.Sound.CLICK);});
        scene.addEventFilter(KeyEvent.KEY_PRESSED,this::shortcut);
        scene.addEventFilter(MouseEvent.ANY,this::skipNarration);
        primaryStage.setTitle(Constants.APP_TITLE);primaryStage.setMinWidth(980);primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);primaryStage.setWidth(settings.width());primaryStage.setHeight(settings.height());
        showHome();primaryStage.show();applyPreferences();
    }
    private void skipNarration(MouseEvent event){
        if(!screen.equals("STORY")){skipGesture=false;return;}
        if(event.getButton()!=MouseButton.PRIMARY)return;
        if(!(scene.lookup("#dialogue-reader") instanceof DialoguePager reader))return;
        if(event.getEventType()==MouseEvent.MOUSE_PRESSED&&!reader.isFinished()){reader.advance();skipGesture=true;event.consume();}
        else if(skipGesture&&(event.getEventType()==MouseEvent.MOUSE_RELEASED||event.getEventType()==MouseEvent.MOUSE_CLICKED)){event.consume();if(event.getEventType()==MouseEvent.MOUSE_CLICKED)skipGesture=false;}
        else if(event.getEventType()==MouseEvent.MOUSE_CLICKED&&!reader.isFinished()){reader.advance();event.consume();}
    }
    private void shortcut(KeyEvent event){
        if(event.isAltDown()||event.isControlDown()||event.isMetaDown())return;
        switch(event.getCode()){
            case F1->runSafely(this::showHelp);
            case F5->runSafely(()->showSaves(true,""));
            case F9->runSafely(()->showSaves(false,""));
            case F11->runSafely(()->setFullscreen(!primaryStage.isFullScreen()));
            case ESCAPE->runSafely(()->{if(primaryStage.isFullScreen())setFullscreen(false);else if(screen.equals("SETTINGS")||screen.equals("HELP")||screen.equals("SAVES")||screen.equals("MENU"))back();else if(screen.equals("STORY")&&!gameService.getCurrentState().getCampaign().getDialogueNode().isBlank()){gameService.performAction("dialogue_close");showStory();}else showMenu();});
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
    public void announce(String message){
        if(message==null||message.isBlank()||toast==null)return;
        if(message.equals(lastToast)&&System.currentTimeMillis()-lastToastAt<700)return;
        lastToast=message;lastToastAt=System.currentTimeMillis();if(toastTimer!=null)toastTimer.stop();
        String visible=UiText.tr(message.length()>210?message.substring(0,207)+"…":message);
        if(scene.lookup(".action-feedback") instanceof Label feedback){
            feedback.setText(visible);toast.setVisible(false);toast.setManaged(false);
            toastTimer=new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.5));toastTimer.setOnFinished(e->feedback.setText(""));toastTimer.play();return;
        }
        toast.setText(visible);toast.setVisible(true);toast.setManaged(true);
        toastTimer=new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.5));toastTimer.setOnFinished(e->{toast.setVisible(false);toast.setManaged(false);});toastTimer.play();
    }
    private void savedFeedback(){
        if(gameService.saveRevision()<=seenSaveRevision)return;seenSaveRevision=gameService.saveRevision();
        saveMessage="Saved";if(saveIndicator!=null)saveIndicator.setText(UiText.tr(saveMessage));
        if(saveTimer!=null)saveTimer.stop();saveTimer=new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));saveTimer.setOnFinished(e->{saveMessage="";if(saveIndicator!=null)saveIndicator.setText("");});saveTimer.play();
        announce(gameService.lastFeedback());
    }
    private String footerHint(){
        if(screen.equals("HOME"))return "↑ ↓ Select  ·  Enter Confirm";
        if(screen.equals("STORY"))return "Click: reveal text  ·  1–9: actions  ·  Esc: Menu";
        if(screen.equals("INVENTORY"))return "Enter: details  ·  Right click: actions  ·  Esc: Menu";
        if(screen.equals("ADVENTURE:0"))return "Select a connected location  ·  Esc: Menu";
        if(screen.equals("ADVENTURE:2"))return "Select goods to inspect  ·  Use Buy to purchase";
        if(screen.equals("ADVENTURE:7"))return "Review preparation and entry requirements before entering";
        if(screen.equals("MENU"))return "Choose a category  ·  Esc: return to your journey";
        if(screen.equals("SETTINGS"))return "Changes are saved automatically  ·  Esc: Back";
        return "Tab: navigate  ·  Enter: select  ·  Esc: Back";
    }
    private void footer(){
        Label hint=new Label(UiText.tr(footerHint()));hint.setId("context-controls");hint.setWrapText(true);hint.getStyleClass().add("control-hint");hint.setMinWidth(0);HBox.setHgrow(hint,Priority.ALWAYS);hint.setMaxWidth(Double.MAX_VALUE);
        saveIndicator=new Label(UiText.tr(saveMessage));saveIndicator.setId("save-status");saveIndicator.getStyleClass().add("save-status");saveIndicator.setMinWidth(70);
        Button mute=new Button(UiText.tr(audio.isMuted()?"Muted":"Sound"));mute.setId("quick-mute");mute.getStyleClass().addAll("game-button","quiet-button");mute.setTooltip(new Tooltip(UiText.tr("Music and SFX volume are in Settings.")));mute.setOnAction(e->runSafely(()->{audio.setMuted(!audio.isMuted());footer();refreshUi();}));
        HBox row=new HBox(16,hint,saveIndicator,mute);row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);row.setPadding(new javafx.geometry.Insets(6,18,6,18));row.getStyleClass().add("context-footer");row.setId("context-footer");shell.setBottom(row);
    }
    public String headerContext(String title){
        if(!gameService.hasCurrentGame())return "TELEKNIA";
        var state=gameService.getCurrentState();
        if(title.equals("CAMP MARKET")||title.equals("CAMP & BONDS")&&recall("codex.6.category","Companions").equals("Crafting"))return UiText.format("Gold: {0}",state.getCharacter().getGold());
        if(title.equals("QUEST JOURNAL"))return UiText.format("Active quests: {0}",WorldContent.QUESTS.keySet().stream().filter(id->!state.getCampaign().has(id)).count());
        if(title.equals("Inventory"))return UiText.format("Item types: {0}",state.getInventory().size());
        if(isLocationTitle(title))return WorldContent.location(title).region();
        return "Menu / "+UiText.tr(title);
    }
    public boolean isLocationTitle(String title){return WorldContent.LOCATIONS.values().stream().anyMatch(l->l.name().equals(title));}
    public void requestQuit(){primaryStage.close();}
    private void setView(String id,Parent parent){
        if(!returning&&!java.util.Set.of("SAVES","HELP","SETTINGS","MENU").contains(id))history.clear();
        screen=id;
        if(toastTimer!=null)toastTimer.stop();lastToast="";if(toast!=null){toast.setVisible(false);toast.setManaged(false);}
        String sprite=gameService.hasCurrentGame()?PixelAssets.location(gameService.getCurrentState().getCurrentLocation()):"field_trail";
        backdrop.setImage(PixelAssets.view(sprite).getImage());shell.setCenter(parent);footer();refreshUi();
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
    public void showHome(){menuReturn="HOME";setView("HOME",new HomeController(this,gameService).createView());audio.playMusic(true);}
    public void showStory(){showStory("");}
    public void showStory(String animation){menuReturn="STORY";setView("STORY",new StoryController(this,gameService,animation).createView());audio.playScene(WorldContent.location(gameService.getCurrentState().getCurrentLocation()).region(),gameService.getCurrentState().getCampaign().getEnemy()==null?null:gameService.getCurrentState().getCampaign().getEnemy().getId());}
    public void showMenu(){setView("MENU",new MenuController(this,gameService).createView());}
    public void showInventory(){setView("INVENTORY",new InventoryController(this,gameService).createView());}
    public void showProfile(){setView("PROFILE",new ProfileController(this,gameService).createView());}
    public void showAdventure(){showAdventure(Integer.parseInt(recall("codex.section","0")),"");}
    public void showAdventure(int tab,String message){
        Parent view=new AdventureController(this,gameService,tab).createView();setView("ADVENTURE:"+tab,view);
        if(!message.isBlank())javafx.application.Platform.runLater(()->{if(view.getScene()!=null)announce(message);});
    }
    public void showSaves(boolean saving,String message){overlay("SAVES",new SavesController(this,gameService,saving,message).createView());}
    public void showSettings(){overlay("SETTINGS",new SettingsController(this).createView());}
    public void showHelp(){showTutorial(0);}
    public void showTutorial(int step){overlay("HELP",new TutorialController(this,step).createView());}
    public void back(){if(screen.equals("MENU")){navigate(menuReturn);return;}String target=history.isEmpty()?"HOME":history.pop();returning=true;try{navigate(target);}finally{returning=false;}}
    private void navigate(String target){
        if(target.startsWith("ADVENTURE:")){showAdventure(Integer.parseInt(target.substring(10)),"");return;}
        switch(target){case "MENU"->showMenu();case "STORY"->showStory();case "INVENTORY"->showInventory();case "PROFILE"->showProfile();case "SETTINGS"->showSettings();case "HELP"->showTutorial(0);case "SAVES"->showSaves(false,"");default->showHome();}
    }
    public boolean confirm(String title,String message){
        Alert dialog=new Alert(Alert.AlertType.CONFIRMATION);dialog.initOwner(primaryStage);dialog.setTitle(UiText.tr(title));dialog.setHeaderText(null);dialog.setContentText(UiText.tr(message));
        ButtonType yes=new ButtonType(UiText.tr("Confirm"),ButtonBar.ButtonData.OK_DONE),cancel=new ButtonType(UiText.tr("Cancel"),ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(yes,cancel);dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        return dialog.showAndWait().filter(yes::equals).isPresent();
    }
    public void runSafely(Runnable action){
        try{action.run();savedFeedback();}catch(RuntimeException error){
            if(error instanceof SaveGameRepository.SaveProblem){if(saveTimer!=null)saveTimer.stop();saveMessage="Not saved";if(saveIndicator!=null)saveIndicator.setText(UiText.tr(saveMessage));}
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
        if(toastTimer!=null)toastTimer.stop();if(saveTimer!=null)saveTimer.stop();
        if(audio!=null)audio.close();
        if(settings!=null&&primaryStage!=null&&!primaryStage.isFullScreen()){
            try{settings.setWindowSize(primaryStage.getWidth(),primaryStage.getHeight());}catch(RuntimeException error){System.getLogger(MainApp.class.getName()).log(System.Logger.Level.WARNING,"Window preferences not saved",error);}
        }
    }
    public static void main(String[] args){launch(args);}
}
