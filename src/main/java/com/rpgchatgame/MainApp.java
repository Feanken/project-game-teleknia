package com.rpgchatgame;

import com.rpgchatgame.controller.HomeController;
import com.rpgchatgame.controller.AdventureController;
import com.rpgchatgame.controller.InventoryController;
import com.rpgchatgame.controller.ProfileController;
import com.rpgchatgame.controller.StoryController;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.service.AudioService;
import com.rpgchatgame.ui.AudioControls;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ButtonBase;
import javafx.event.ActionEvent;
import com.rpgchatgame.util.Constants;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private Stage primaryStage;
    private Scene scene;
    private GameService gameService;
    private AudioService audio;
    private BorderPane shell;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.gameService = new GameService();

        audio = new AudioService();
        shell = new BorderPane(new HomeController(this, gameService).createView());
        shell.setBottom(new AudioControls(audio));
        scene = new Scene(shell, 1180, 820);
        scene.addEventFilter(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof ButtonBase button
                    && !button.getStyleClass().contains("toggle-button")) {
                audio.play(AudioService.Sound.CLICK);
            }
        });
        String css = getClass().getResource("/styles/fantasy.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle(Constants.APP_TITLE);
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.show();
        audio.playMusic(true);
    }

    public void showHome() {
        setView(new HomeController(this, gameService).createView());
        audio.playMusic(true);
    }

    public void showStory() {
        setView(new StoryController(this, gameService).createView());
        audio.playMusic(false);
    }

    public void showInventory() {
        setView(new InventoryController(this, gameService).createView());
    }

    public void showProfile() {
        setView(new ProfileController(this, gameService).createView());
    }

    public void showAdventure() { showAdventure(0, ""); }

    public void showAdventure(int tab, String message) {
        Parent view = new AdventureController(this, gameService, tab).createView();
        setView(view);
        if (!message.isBlank()) {
            javafx.application.Platform.runLater(() -> {
                var feedback = view.lookup(".feedback-label");
                if (feedback instanceof javafx.scene.control.Label label) label.setText(message);
            });
        }
    }

    public void runSafely(Runnable action) {
        try { action.run(); }
        catch (RuntimeException error) {
            System.getLogger(MainApp.class.getName()).log(System.Logger.Level.ERROR, "Game action failed", error);
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.initOwner(primaryStage);
            alert.setTitle("Unable to complete action");
            alert.setHeaderText("Your previous save was kept.");
            alert.setContentText(error.getMessage());
            alert.showAndWait();
        }
    }

    private void setView(Parent parent) {
        shell.setCenter(parent);
    }

    public AudioService getAudio() { return audio; }

    @Override
    public void stop() {
        if (audio != null) audio.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
