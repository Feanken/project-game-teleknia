package com.rpgchatgame.controller;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.GameService;
import com.rpgchatgame.ui.PixelAssets;
import com.rpgchatgame.util.Constants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class HomeController {
    private final MainApp app;
    private final GameService gameService;

    public HomeController(MainApp app, GameService gameService) {
        this.app = app;
        this.gameService = gameService;
    }

    public Parent createView() {
        var root=new javafx.scene.layout.StackPane();root.getStyleClass().add("home-cinematic");root.setId("title-screen");
        var image=new javafx.scene.image.Image(getClass().getResource("/art/title-campfire.png").toExternalForm());
        var background=new javafx.scene.image.ImageView(image);background.setManaged(false);background.setMouseTransparent(true);background.setPreserveRatio(true);background.setSmooth(false);
        double ratio=image.getWidth()/image.getHeight();background.fitWidthProperty().bind(javafx.beans.binding.Bindings.max(root.widthProperty(),root.heightProperty().multiply(ratio)));
        background.layoutXProperty().bind(root.widthProperty().subtract(background.fitWidthProperty()).divide(2));background.layoutYProperty().bind(root.heightProperty().subtract(background.fitWidthProperty().divide(ratio)).divide(2));
        javafx.scene.layout.Region shade=new javafx.scene.layout.Region();shade.getStyleClass().add("title-shade");shade.setMouseTransparent(true);
        VBox preview=new VBox(6);preview.getStyleClass().add("title-preview");preview.setId("title-preview");preview.setMinHeight(110);
        var slots=gameService.saveSlots();var auto=slots.getFirst();boolean continuable=auto.occupied()&&!auto.corrupt();
        VBox choices=new VBox(3);java.util.List<Button> options=new java.util.ArrayList<>();
        java.util.function.BiConsumer<Button,Runnable> select=(selected,update)->{for(Button option:options){option.getStyleClass().remove("title-option-active");if(option.getGraphic()!=null)option.getGraphic().setOpacity(option==selected?1:0);}selected.getStyleClass().add("title-option-active");update.run();app.refreshUi();};
        java.util.function.BiFunction<String,Runnable,Button> option=(label,run)->{Button button=new Button(label);button.getStyleClass().addAll("game-button","title-option");button.setMaxWidth(Double.MAX_VALUE);Label marker=new Label("›");marker.getStyleClass().add("title-marker");marker.setOpacity(0);button.setGraphic(marker);button.setOnAction(e->app.runSafely(run));options.add(button);choices.getChildren().add(button);return button;};
        Button cont=option.apply("Continue Story",()->gameService.continueStory().ifPresentOrElse(state->app.showStory(),()->app.showSaves(false,"")));cont.setId("title-continue");cont.setDisable(!continuable);
        Button start=option.apply("New Journey",this::newAdventure);start.setId("title-new");
        Button load=option.apply("Load Game",()->app.showSaves(false,""));load.setId("title-load");load.setDisable(!gameService.hasSavedGame());
        Button settings=option.apply("Settings",app::showSettings);settings.setId("title-settings");Button quit=option.apply("Quit",app::requestQuit);quit.setId("title-quit");
        java.util.Map<Button,Runnable> descriptions=new java.util.HashMap<>();
        descriptions.put(cont,()->{
            preview.getChildren().setAll(titleText(auto.name(),"title-save-name"),titleText(auto.characterClass()+" / Lv "+auto.level(),"title-meta"),titleText(auto.location(),"title-meta"));preview.getChildren().getFirst().getProperties().put("literal",true);
            try{var date=java.time.Instant.parse(auto.savedAt()).atZone(java.time.ZoneId.of("Asia/Jakarta"));preview.getChildren().add(titleText(com.rpgchatgame.util.UiText.tr("Last saved")+" · "+java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm",java.util.Locale.forLanguageTag(com.rpgchatgame.util.UiText.language().equals("id")?"id-ID":"en-GB")).format(date),"title-meta"));}catch(Exception ignored){}
        });
        descriptions.put(start,()->preview.getChildren().setAll(titleText("Every road begins with a choice.","title-save-name"),titleText("Choose your class. Find your companions. Decide what the Crown will become.","title-meta"),titleText("Only autosave is replaced. Manual checkpoints remain safe.","title-meta")));
        descriptions.put(load,()->preview.getChildren().setAll(titleText("Return to a remembered road.","title-save-name"),titleText("Choose an autosave, a manual checkpoint, or recover a backup.","title-meta")));
        descriptions.put(settings,()->preview.getChildren().setAll(titleText("Make this journey your own.","title-save-name"),titleText("Language, display, reduced motion, music and sound effects.","title-meta")));
        descriptions.put(quit,()->preview.getChildren().setAll(titleText("Until the next campfire.","title-save-name"),titleText("Your committed choices are already autosaved.","title-meta")));
        for(Button button:options){button.setOnMouseEntered(e->{if(!button.isDisabled())select.accept(button,descriptions.get(button));});button.focusedProperty().addListener((o,old,focused)->{if(focused)select.accept(button,descriptions.get(button));});}
        Label logo=titleText("TELEKNIA","title-logo");Label chapter=titleText("ASHES OF THE NORTHERN GATE","title-chapter");
        VBox content=new VBox(12,logo,chapter,choices,preview);content.setMaxWidth(420);content.setPadding(new Insets(26));content.setAlignment(Pos.CENTER_LEFT);
        var scroll=new javafx.scene.control.ScrollPane(content);scroll.setFitToWidth(true);scroll.setMaxWidth(440);scroll.setMaxHeight(Double.MAX_VALUE);scroll.getStyleClass().add("title-scroll");
        BorderPane overlay=new BorderPane();overlay.setLeft(scroll);BorderPane.setMargin(scroll,new Insets(24,0,12,28));
        Label version=titleText("v"+Constants.APP_VERSION+"  /  OFFLINE ADVENTURE","title-version");BorderPane.setAlignment(version,Pos.CENTER_RIGHT);BorderPane.setMargin(version,new Insets(0,22,12,22));overlay.setBottom(version);
        root.getChildren().addAll(background,shade,overlay);var clip=new javafx.scene.shape.Rectangle();clip.widthProperty().bind(root.widthProperty());clip.heightProperty().bind(root.heightProperty());root.setClip(clip);
        if(!app.getSettings().reducedMotion()&&System.getProperty("teleknia.uiCheck.root")==null){
            var fade=new javafx.animation.FadeTransition(javafx.util.Duration.millis(500),content);fade.setFromValue(0);fade.setToValue(1);fade.play();root.sceneProperty().addListener((o,old,next)->{if(next==null)fade.stop();});
            for(int i=0;i<6;i++){var ember=new javafx.scene.shape.Circle(1.3,javafx.scene.paint.Color.web("#e7b879"));ember.setManaged(false);ember.setMouseTransparent(true);ember.layoutXProperty().bind(background.layoutXProperty().add(background.fitWidthProperty().multiply(.83+i*.004)));ember.layoutYProperty().bind(background.layoutYProperty().add(background.fitWidthProperty().divide(ratio).multiply(.67)));root.getChildren().add(ember);
                var rise=new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(2.4+i*.25),ember);rise.setFromY(0);rise.setToY(-45-i*5);rise.setCycleCount(javafx.animation.Animation.INDEFINITE);rise.setDelay(javafx.util.Duration.seconds(i*.3));
                var glow=new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2.4+i*.25),ember);glow.setFromValue(.8);glow.setToValue(0);glow.setCycleCount(javafx.animation.Animation.INDEFINITE);glow.setDelay(javafx.util.Duration.seconds(i*.3));var motion=new javafx.animation.ParallelTransition(rise,glow);motion.play();root.sceneProperty().addListener((o,old,next)->{if(next==null)motion.stop();});
            }
        }
        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,e->{
            if(e.getCode()==javafx.scene.input.KeyCode.UP||e.getCode()==javafx.scene.input.KeyCode.DOWN){var enabled=options.stream().filter(button->!button.isDisabled()).toList();int active=0;for(int j=0;j<enabled.size();j++)if(enabled.get(j).isFocused()||enabled.get(j).getStyleClass().contains("title-option-active"))active=j;int next=Math.floorMod(active+(e.getCode()==javafx.scene.input.KeyCode.UP?-1:1),enabled.size());enabled.get(next).requestFocus();select.accept(enabled.get(next),descriptions.get(enabled.get(next)));e.consume();}
            else if(e.getCode()==javafx.scene.input.KeyCode.ENTER&&root.getScene().getFocusOwner() instanceof Button focused&&options.contains(focused)){focused.fire();e.consume();}
        });
        select.accept(continuable?cont:start,descriptions.get(continuable?cont:start));return root;
    }
    private Label titleText(String text,String style){Label label=new Label(text);label.setWrapText(true);label.getStyleClass().add(style);return label;}

    private void newAdventure() {
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("New Adventure");
        if (app.getAudio() != null) dialog.initOwner(javafx.stage.Window.getWindows().stream().filter(javafx.stage.Window::isShowing).findFirst().orElse(null));
        var start = new javafx.scene.control.ButtonType("Begin adventure", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(start, javafx.scene.control.ButtonType.CANCEL);
        var name = new javafx.scene.control.TextField("Kael");
        name.getStyleClass().add("form-field");
        var klass = new javafx.scene.control.ComboBox<String>();
        klass.getItems().addAll("Fighter", "Rogue", "Mage"); klass.setValue("Fighter"); klass.setId("character-class");
        Label details = new Label(); details.setWrapText(true); details.setMaxWidth(440);
        Runnable update = () -> details.setText(switch (klass.getValue()) {
            case "Rogue" -> "88 HP / 30 MP. High agility. Shadow Strike combines weapon power and agility; attacks on every fourth combat turn can critically strike.";
            case "Mage" -> "80 HP / 55 MP. High intelligence. Ember Surge ignores armor. Mend restores more HP.";
            default -> "100 HP / 30 MP. Strong defense. Power Strike doubles your equipped attack before enemy armor.";
        });
        klass.valueProperty().addListener((o,b,a) -> update.run()); update.run();
        var difficulty = new javafx.scene.control.ComboBox<String>();
        difficulty.getItems().addAll("STANDARD", "STORY"); difficulty.setValue("STANDARD"); difficulty.setId("new-difficulty");
        difficulty.setConverter(new javafx.util.StringConverter<>() {
            public String toString(String value) { return com.rpgchatgame.util.UiText.tr(value.equals("STORY") ? "Story difficulty" : "Standard difficulty"); }
            public String fromString(String value) { return value; }
        });
        Label notice = new Label("This replaces autosave only. Manual slots stay safe.");
        notice.setWrapText(true); notice.setMaxWidth(440);
        var tutorial = new javafx.scene.control.CheckBox("Show the beginner tutorial");
        tutorial.setSelected(!app.getSettings().tutorialSeen());
        VBox form = new VBox(12, new Label("Character name (1-24 characters)"), name, new Label("Class"), klass, details, new Label("Difficulty"), difficulty, new Label("Story reduces incoming damage and gold penalties. Standard keeps the normal combat rules."), notice, tutorial);
        form.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        dialog.getDialogPane().lookupButton(start).disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> name.getText().isBlank() || name.getText().trim().length() > 24, name.textProperty()));
        var localizedCancel = new javafx.scene.control.ButtonType(com.rpgchatgame.util.UiText.tr("Cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().set(1, localizedCancel);
        dialog.setTitle(com.rpgchatgame.util.UiText.tr("New Adventure"));
        com.rpgchatgame.ui.UiLocalization.apply(dialog.getDialogPane());
        klass.setOnAction(event -> { update.run(); com.rpgchatgame.ui.UiLocalization.apply(dialog.getDialogPane()); });
        dialog.setResultConverter(type -> type == start);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(ok -> app.runSafely(() -> {
            gameService.startNewStory(name.getText(), klass.getValue(), difficulty.getValue()); app.showStory();
            if (tutorial.isSelected()) app.showHelp();
        }));
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "primary-button");
        button.setMinWidth(240);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("game-button", "secondary-button");
        button.setMinWidth(240);
        return button;
    }
}
