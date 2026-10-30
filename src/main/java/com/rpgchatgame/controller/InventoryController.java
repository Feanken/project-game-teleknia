package com.rpgchatgame.controller;
import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.ui.*;
import com.rpgchatgame.util.UiText;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Supplier;
import static com.rpgchatgame.ui.RpgUi.*;

/** Thirty slots per page; selection is read-only, actions are explicit and transactional. */
public final class InventoryController {
    private final MainApp app;private final GameService game;
    private final GridPane grid=new GridPane();private final VBox paper=new VBox(10),detail=new VBox(9);
    private final Label feedback=text(""),pageLabel=text("");
    private final ComboBox<String> filter=new ComboBox<>(),sorting=new ComboBox<>();
    private final Map<String,Button> slots=new HashMap<>();
    private String selected;private int page;private Button prev,next;
    public InventoryController(MainApp app,GameService game){this.app=app;this.game=game;selected=app.recall("inventory.selected","");page=Integer.parseInt(app.recall("inventory.page","0"));}
    public Parent createView(){
        BorderPane root=new BorderPane();root.getStyleClass().addAll("app-root","parchment-shell");root.setTop(nav(app,"Inventory"));
        filter.getItems().setAll("All","Quest Items");filter.setValue(app.recall("inventory.filter","All"));filter.setId("inventory-filter");
        sorting.getItems().setAll("Name","Rarity","Quantity");sorting.setValue(app.recall("inventory.sort","Name"));sorting.setId("inventory-sort");
        var converter=new javafx.util.StringConverter<String>(){public String toString(String v){return UiText.tr(v);}public String fromString(String v){return v;}};filter.setConverter(converter);sorting.setConverter(converter);
        filter.setOnAction(e->{page=0;app.remember("inventory.filter",filter.getValue());refresh();});sorting.setOnAction(e->{page=0;app.remember("inventory.sort",sorting.getValue());refresh();});
        HBox controls=new HBox(8,filter,sorting);controls.setAlignment(Pos.CENTER_LEFT);
        grid.setHgap(4);grid.setVgap(4);grid.setAlignment(Pos.TOP_CENTER);grid.setId("inventory-grid");
        for(int i=0;i<5;i++){ColumnConstraints c=new ColumnConstraints();c.setPercentWidth(20);c.setHgrow(Priority.ALWAYS);grid.getColumnConstraints().add(c);}
        prev=button(app,"Previous",()->{page--;refresh();});next=button(app,"Next",()->{page++;refresh();});prev.setId("inventory-prev");next.setId("inventory-next");
        HBox pages=new HBox(10,prev,pageLabel,next);pages.setAlignment(Pos.CENTER);
        VBox bag=new VBox(10,controls,scroll(grid),pages);bag.getStyleClass().add("satchel-panel");bag.setMinWidth(380);VBox.setVgrow(bag.getChildren().get(1),Priority.ALWAYS);
        VBox left=new VBox(12,paper,detail);left.setPadding(new Insets(12));left.getStyleClass().add("paper-doll-panel");ScrollPane leftScroll=scroll(left);leftScroll.setPrefWidth(320);leftScroll.setMinWidth(290);leftScroll.setMaxWidth(360);
        HBox body=new HBox(12,leftScroll,bag);HBox.setHgrow(bag,Priority.ALWAYS);body.setPadding(new Insets(12));root.setCenter(body);
        feedback.getStyleClass().addAll("feedback-label","action-feedback");feedback.setId("inventory-feedback");BorderPane.setMargin(feedback,new Insets(0,14,6,14));root.setBottom(feedback);
        root.getProperties().put("prev",prev);root.getProperties().put("next",next);refresh();return root;
    }
    private boolean equipment(Item item){return Set.of(Item.ItemType.WEAPON,Item.ItemType.ARMOR,Item.ItemType.ACCESSORY).contains(item.getType());}
    private String description(InventoryItem row){var i=row.getItem();return i.getName()+" x"+row.getQuantity()+"\n"+UiText.tr(i.getType().name())+" / "+UiText.tr(i.getRarity().name())+"\n"+i.getDescription()+"\n"+i.getEffect()+"\n"+InventoryQuery.comparison(game.getCurrentState(),i.getId())+(AtlasService.equipped(game.getCurrentState(),i.getId())?"\n"+UiText.tr("Equipped"):"");}
    private void refresh(){
        var s=game.getCurrentState();slots.clear();grid.getChildren().clear();
        var rows=InventoryQuery.select(s.getInventory(),filter.getValue().equals("All")?null:Item.ItemType.QUEST,InventoryQuery.Sort.valueOf(sorting.getValue().toUpperCase(java.util.Locale.ROOT)));
        int pages=Math.max(1,(rows.size()+29)/30);page=Math.max(0,Math.min(page,pages-1));app.remember("inventory.page",Integer.toString(page));
        pageLabel.setText((page+1)+" / "+pages+"  ·  "+rows.size());
        if(prev!=null)prev.setDisable(page==0);if(next!=null)next.setDisable(page>=pages-1);
        for(int n=0;n<30;n++){
            int index=page*30+n;Button slot=new Button();slot.setMaxWidth(Double.MAX_VALUE);slot.setMinSize(68,68);slot.setPrefHeight(68);slot.setPadding(Insets.EMPTY);slot.getStyleClass().add("inventory-slot");
            if(index<rows.size()){
                var row=rows.get(index);var item=row.getItem();String id=item.getId();slot.setId("item-"+id);slots.put(id,slot);
                Label count=text((AtlasService.equipped(s,id)?"E  ":"")+"x"+row.getQuantity());count.getStyleClass().add("stack-count");count.setMouseTransparent(true);
                StackPane image=new StackPane(PixelAssets.view(PixelAssets.item(id)),count);StackPane.setAlignment(count,Pos.BOTTOM_RIGHT);slot.setGraphic(image);
                slot.getStyleClass().add("rarity-"+item.getRarity().name().toLowerCase());slot.setAccessibleText(item.getName()+" x"+row.getQuantity());
                focusTip(slot,tip(description(row)));slot.setOnAction(e->select(id));
                ContextMenu menu=new ContextMenu();
                if(equipment(item)){boolean equipped=AtlasService.equipped(s,id);MenuItem equip=new MenuItem(UiText.tr(equipped?"Unequip":"Equip"));equip.setId("context-equip-"+id);equip.setDisable(!AtlasService.inventoryLock(s).isBlank());equip.setOnAction(e->act(()->equipped?game.unequipItem(id):game.equipItem(id)));menu.getItems().add(equip);}
                else if(item.getType()==Item.ItemType.CONSUMABLE){MenuItem use=new MenuItem(UiText.tr("Use"));use.setDisable(!AtlasService.inventoryLock(s).isBlank());use.setOnAction(e->act(()->game.useItem(id)));menu.getItems().add(use);}
                MenuItem remove=new MenuItem(UiText.tr("Remove"));remove.setDisable(!AtlasService.removeBlock(s,id).isBlank());remove.setOnAction(e->app.runSafely(()->remove(id)));menu.getItems().add(remove);slot.setContextMenu(menu);
            }else{slot.setDisable(true);slot.getStyleClass().add("empty-slot");slot.setAccessibleText(UiText.tr("Empty slot"));}
            grid.add(slot,n%5,n/5);
        }
        if(!rows.stream().anyMatch(r->r.getItem().getId().equals(selected)))selected=rows.isEmpty()?"":rows.getFirst().getItem().getId();
        drawPaper();select(selected);app.refreshUi();
    }
    private void select(String id){
        selected=id;app.remember("inventory.selected",id);slots.forEach((key,b)->{b.getStyleClass().remove("slot-selected");if(key.equals(id))b.getStyleClass().add("slot-selected");});
        detail.getChildren().clear();var s=game.getCurrentState();var row=s.getInventory().stream().filter(i->i.getItem().getId().equals(id)).findFirst().orElse(null);
        if(row==null){detail.getChildren().add(text("No matching items"));return;}
        var item=row.getItem();detail.getChildren().addAll(title(item.getName()),text("x"+row.getQuantity()+" / "+UiText.tr(item.getType().name())+" / "+UiText.tr(item.getRarity().name())),text(item.getDescription()),text(item.getEffect()),text(InventoryQuery.comparison(s,id)));boolean equipped=AtlasService.equipped(s,id);String lock=AtlasService.inventoryLock(s);
        if(equipment(item)||item.getType()==Item.ItemType.CONSUMABLE){Button action=button(app,equipment(item)?equipped?"Unequip":"Equip":"Use",()->act(()->equipment(item)?equipped?game.unequipItem(id):game.equipItem(id):game.useItem(id)));action.setId("inventory-primary");action.setDisable(!lock.isBlank());detail.getChildren().add(action);}
        Button remove=button(app,"Remove",()->remove(id));remove.setId("inventory-remove");String blocked=AtlasService.removeBlock(s,id);remove.setDisable(!blocked.isBlank());detail.getChildren().addAll(remove,text(!lock.isBlank()?lock:blocked));
    }
    private void drawPaper(){
        paper.getChildren().clear();var s=game.getCurrentState();var w=s.getCampaign();Label name=title(s.getCharacter().getName());name.getProperties().put("literal",true);
        GridPane doll=new GridPane();doll.setHgap(6);doll.setVgap(8);doll.setAlignment(Pos.CENTER);doll.setId("paper-doll");
        doll.add(equipSlot("Weapon",w.getWeapon()),0,0);doll.add(PixelAssets.view("field_"+s.getCharacter().getCharacterClass().toLowerCase(java.util.Locale.ROOT),2),1,0,1,2);doll.add(equipSlot("Armor",w.getArmor()),2,0);doll.add(equipSlot("Accessory",w.getAccessory()),2,1);
        paper.getChildren().addAll(name,doll,text(s.getCharacter().getCharacterClass()+" / Lv "+s.getCharacter().getLevel()),text("HP "+s.getCharacter().getHp()+" / "+s.getCharacter().getMaxHp()+"  MP "+s.getCharacter().getMp()+" / "+s.getCharacter().getMaxMp()),text("Attack "+ItemCatalog.attack(s)+"  /  Defense "+ItemCatalog.defense(s)+"  /  Gold "+s.getCharacter().getGold()));
    }
    private Button equipSlot(String title,String id){Button b=button(app,"",()->select(id));b.setId("equipped-"+title.toLowerCase(java.util.Locale.ROOT));b.getStyleClass().add("equipment-slot");b.setGraphic(new VBox(3,text(UiText.tr(title)),id.isBlank()?text("—"):PixelAssets.view(PixelAssets.item(id))));b.setDisable(id.isBlank());if(!id.isBlank()){
            focusTip(b,tip(ItemCatalog.equippedName(id)+"\n"+InventoryQuery.comparison(game.getCurrentState(),id)));
            MenuItem unequip=new MenuItem(UiText.tr("Unequip"));unequip.setDisable(!AtlasService.inventoryLock(game.getCurrentState()).isBlank());unequip.setOnAction(e->act(()->game.unequipItem(id)));b.setContextMenu(new ContextMenu(unequip));
        }return b;}
    private void act(Supplier<String> action){app.runSafely(()->{feedback.setText(action.get());refresh();});}
    private void remove(String id){
        var s=game.getCurrentState();String blocked=AtlasService.removeBlock(s,id);if(!blocked.isBlank()){feedback.setText(blocked);return;}
        int max=BondService.quantity(s,id);Dialog<Integer> dialog=new Dialog<>();dialog.initOwner(app.getWindow());dialog.setTitle(UiText.tr("Remove item"));dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/fantasy.css").toExternalForm());
        Spinner<Integer> amount=new Spinner<>(1,max,1);amount.setId("remove-quantity");amount.setEditable(false);
        dialog.getDialogPane().setContent(new VBox(12,text(ItemCatalog.equippedName(id)),text(UiText.tr("Discard without gold. This cannot be undone.")),amount));
        ButtonType confirm=new ButtonType(UiText.tr("Remove"),ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().setAll(confirm,new ButtonType(UiText.tr("Cancel"),ButtonBar.ButtonData.CANCEL_CLOSE));dialog.setResultConverter(b->b==confirm?amount.getValue():null);
        dialog.showAndWait().ifPresent(quantity->act(()->game.removeItem(id,quantity)));
    }
}
