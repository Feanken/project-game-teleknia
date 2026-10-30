package com.rpgchatgame.ui;
import com.rpgchatgame.MainApp;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import java.util.*;
import java.util.function.*;
import static com.rpgchatgame.ui.RpgUi.*;

/** Shared browse/detail layout: choosing a card never performs its action. */
public final class CollectionView extends BorderPane {
    public record Entry(String id,String title,String sprite,String summary,Supplier<Node> detail){}
    private final MainApp app;private final String key;private final VBox detail=new VBox(12);private final TilePane cards=new TilePane(8,8);private List<Entry> rows=List.of();private String category;private ScrollPane listScroll;
    private final Map<String,Button> buttons=new HashMap<>();
    public CollectionView(MainApp app,String key,Map<String,List<Entry>> groups){
        this.app=app;this.key=key;setMinHeight(0);getStyleClass().add("collection-view");
        ComboBox<String> categories=new ComboBox<>();categories.getItems().addAll(groups.keySet());categories.setId("collection-category");categories.setConverter(new javafx.util.StringConverter<>(){public String toString(String s){return com.rpgchatgame.util.UiText.tr(s);}public String fromString(String s){return s;}});
        String selected=app.recall(key+".category",categories.getItems().getFirst());if(!groups.containsKey(selected))selected=categories.getItems().getFirst();categories.setValue(selected);category=selected;
        categories.setOnAction(e->{app.remember(key+".category",categories.getValue());category=categories.getValue();populate(groups.get(categories.getValue()));});
        listScroll=scroll(cards);listScroll.setMinHeight(0);listScroll.vvalueProperty().addListener((o,a,b)->app.remember(key+".scroll."+category,b.toString()));VBox list=new VBox(10,categories,listScroll);VBox.setVgrow(list.getChildren().get(1),Priority.ALWAYS);list.setMinWidth(180);list.setPrefWidth(254);list.setMaxWidth(270);
        cards.setPrefColumns(2);cards.setPrefTileWidth(108);cards.setTileAlignment(Pos.TOP_CENTER);cards.setPadding(new Insets(3));cards.setId("collection-cards");
        ScrollPane info=scroll(detail);info.setMinWidth(230);info.setMinHeight(0);detail.getStyleClass().add("codex-detail");detail.setId("collection-detail");
        Label feedback=text("");feedback.getStyleClass().addAll("feedback-label","action-feedback");feedback.setId("codex-feedback");BorderPane panel=new BorderPane(info);panel.setBottom(feedback);HBox body=new HBox(14,list,panel);HBox.setHgrow(panel,Priority.ALWAYS);setCenter(body);setPadding(new Insets(12));populate(groups.get(selected));
    }
    private void populate(List<Entry> entries){
        double scrollValue=Double.parseDouble(app.recall(key+".scroll."+category,"0"));rows=entries;cards.getChildren().clear();buttons.clear();
        for(var e:rows){Button b=button(app,"",()->select(e.id()));b.getStyleClass().add("collection-card");b.setId("entry-"+e.id().replaceAll("[^a-zA-Z0-9_-]","-"));Label name=text(e.title());name.setMaxWidth(104);name.setMinHeight(42);name.setPrefHeight(42);name.setMaxHeight(42);name.getStyleClass().add("collection-name");VBox icon=new VBox(6,RpgUi.icon(e.sprite()),name);icon.setAlignment(Pos.TOP_CENTER);b.setGraphic(icon);b.setMinHeight(148);b.setPrefHeight(148);b.setMaxHeight(148);b.setPrefWidth(108);b.setMinWidth(108);b.setAccessibleText(e.title());focusTip(b,tip(com.rpgchatgame.util.UiText.tr(e.title())+"\n"+com.rpgchatgame.util.UiText.tr(e.summary())));buttons.put(e.id(),b);cards.getChildren().add(b);}
        if(rows.isEmpty()){cards.getChildren().add(text("No entries in this category."));detail.getChildren().setAll(text("Choose another category."));return;}
        String saved=app.recall(key+".selected."+category,rows.getFirst().id());String choice=rows.stream().anyMatch(e->e.id().equals(saved))?saved:rows.getFirst().id();select(choice);listScroll.setVvalue(scrollValue);app.refreshUi();
    }
    private void select(String id){app.remember(key+".selected."+category,id);buttons.forEach((key,b)->{b.getStyleClass().remove("collection-selected");if(key.equals(id))b.getStyleClass().add("collection-selected");});var entry=rows.stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();detail.getChildren().setAll(entry.detail().get());app.refreshUi();}
}
