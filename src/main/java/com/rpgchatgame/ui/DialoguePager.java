package com.rpgchatgame.ui;

import javafx.animation.*;
import javafx.beans.property.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.*;
import com.rpgchatgame.util.UiText;

/** UI-only reading state: revealing/advancing never invokes a game action. */
public final class DialoguePager extends VBox {
    private final Label body=new Label(),hint=new Label();
    private final List<String> pages;
    private final BooleanProperty finished=new SimpleBooleanProperty(false);
    private final Timeline timer=new Timeline();
    private final boolean instant;
    private int page,index;
    public DialoguePager(String text,boolean instant){
        this.instant=instant;this.pages=pages(text);setSpacing(4);setId("dialogue-reader");setFocusTraversable(true);
        body.setWrapText(true);body.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);body.getProperties().put("literal",true);body.getStyleClass().add("narration-text");body.setId("scene-narration");
        hint.getStyleClass().add("item-meta");hint.setId("dialogue-hint");
        getChildren().addAll(body,hint);
        timer.getKeyFrames().add(new KeyFrame(Duration.millis(24),e->tick()));timer.setCycleCount(Animation.INDEFINITE);
        setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY){requestFocus();advance();e.consume();}});
        setOnKeyPressed(e->{if(e.getCode()==KeyCode.SPACE||e.getCode()==KeyCode.ENTER){advance();e.consume();}});
        sceneProperty().addListener((o,oldScene,newScene)->{if(newScene==null)timer.stop();});
        if(instant){body.setText(text);finished.set(true);hint.setText("");}else show();
    }
    public static List<String> pages(String text){return List.of(text);}
    public ReadOnlyBooleanProperty finishedProperty(){return finished;}
    public boolean isFinished(){return finished.get();}
    public String visibleText(){return body.getText();}
    private void show(){index=0;body.setText("");hint.setText(UiText.tr("Click anywhere to reveal"));timer.playFromStart();}
    private void tick(){String text=pages.get(page);if(index<text.length()){index=text.offsetByCodePoints(index,1);body.setText(text.substring(0,index));}if(index>=text.length())revealed();}
    private void revealed(){timer.stop();body.setText(pages.get(page));index=pages.get(page).length();boolean last=page==pages.size()-1;finished.set(last);hint.setText(UiText.tr("Choose your response"));}
    public void advance(){if(instant)return;if(index<pages.get(page).length())revealed();else if(page<pages.size()-1){page++;show();}}
}
