package com.rpgchatgame.ui;
import com.rpgchatgame.MainApp;
import com.rpgchatgame.util.UiText;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.util.Duration;
public final class RpgUi {
    public static Label text(String value){Label l=new Label(value);l.setWrapText(true);l.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);l.managedProperty().bind(l.textProperty().isNotEmpty());l.visibleProperty().bind(l.textProperty().isNotEmpty());return l;}
    public static Label title(String value){Label l=text(value);l.getStyleClass().add("panel-title");return l;}
    public static Button button(MainApp app,String label,Runnable action){Button b=new Button(label);b.getStyleClass().addAll("game-button","nav-button");b.setOnAction(e->app.runSafely(action));return b;}
    public static ScrollPane scroll(Node node){ScrollPane s=new ScrollPane(node);s.setFitToWidth(true);s.getStyleClass().add("plain-scroll");return s;}
    public static HBox nav(MainApp app,String title){
        Label l=title(title);l.getStyleClass().add("screen-title");Label context=text(app.headerContext(title));context.getStyleClass().add("header-context");
        VBox heading=new VBox(2,context,l);heading.setMinWidth(0);HBox.setHgrow(heading,Priority.ALWAYS);
        Button back=button(app,app.isLocationTitle(title)?"Menu":"Back",app::showMenu);back.setId("header-menu");
        HBox h=new HBox(18,heading,back);h.setAlignment(Pos.CENTER_LEFT);h.setPadding(new Insets(10,20,10,20));h.getStyleClass().add("top-nav");return h;
    }
    public static Node icon(String sprite){if(!sprite.isBlank())return PixelAssets.view(sprite);Label q=new Label("?");q.getStyleClass().add("fog-symbol");StackPane fog=new StackPane(q);fog.setMinSize(64,64);fog.setPrefSize(64,64);fog.setStyle("-fx-background-color: #111a23;");return fog;}
    public static Tooltip tip(String text){Tooltip tip=new Tooltip(text);tip.setWrapText(true);tip.setMaxWidth(360);tip.setShowDelay(Duration.seconds(1));tip.setHideDelay(Duration.millis(80));return tip;}
    public static void focusTip(Control control,Tooltip tip){control.setTooltip(tip);control.focusedProperty().addListener((o,a,b)->{if(b&&control.getScene()!=null){var p=control.localToScreen(control.getBoundsInLocal());if(p!=null){var screen=javafx.stage.Screen.getScreensForRectangle(p.getMinX(),p.getMinY(),1,1).getFirst().getVisualBounds();tip.show(control,Math.min(p.getMaxX(),screen.getMaxX()-370),Math.min(p.getMaxY(),screen.getMaxY()-220));}}else tip.hide();});control.sceneProperty().addListener((o,a,b)->{if(b==null)tip.hide();});}
}
