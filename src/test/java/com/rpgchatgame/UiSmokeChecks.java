package com.rpgchatgame;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.*;
import com.rpgchatgame.repository.*;
import javafx.application.Platform;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.event.Event;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;

public final class UiSmokeChecks {
 static MainApp app;static Stage stage;static GameService game;static int checks;
 static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static Parent root(){Parent root=stage.getScene().getRoot();root.applyCss();root.layout();return root;}
 static Button button(Parent parent,String text){parent.applyCss();parent.layout();return parent.lookupAll(".button").stream().filter(n->n instanceof Button b&&b.getText().equals(text)).map(n->(Button)n).findFirst().orElseThrow(()->new AssertionError("Missing button "+text));}
 static Button button(String text){return button(root(),text);}
 static boolean label(String text){root().applyCss();root().layout();return root().lookupAll(".label").stream().anyMatch(n->n instanceof Label l&&l.getText().equals(text));}
 static void key(KeyCode code){Event.fireEvent(stage.getScene(),new KeyEvent(KeyEvent.KEY_PRESSED,"","",code,false,false,false,false));}
 static void later(Runnable action){var pause=new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));pause.setOnFinished(e->Platform.runLater(()->{try{action.run();}catch(Throwable error){finish(error);}}));pause.play();}
 static void capture(String file)throws Exception{
  root().applyCss();root().layout();
  for(var node:root().lookupAll(".chat-scroll"))if(node instanceof ScrollPane pane)pane.setVvalue(1);
  var image=stage.getScene().snapshot(null);var out=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
  for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,image.getPixelReader().getArgb(x,y));
  ImageIO.write(out,"png",new File(file+".png"));
 }
 static Parent dialog(){return Window.getWindows().stream().filter(w->w!=stage&&w.isShowing()).findFirst().orElseThrow().getScene().getRoot();}
 static void answerConfirm(){Platform.runLater(()->{try{button(dialog(),com.rpgchatgame.util.UiText.tr("Confirm")).fire();}catch(Throwable error){finish(error);}});}
 @SuppressWarnings("unchecked") static void create(String name,boolean tutorial){
  Platform.runLater(()->{try{
   var form=dialog();form.applyCss();form.layout();TextField field=(TextField)form.lookup(".text-field");field.setText("");
   Button begin=button(form,"Begin adventure");check(begin.isDisabled(),"Blank name cannot start");field.setText(name);
   ((ComboBox<String>)form.lookup("#character-class")).getSelectionModel().select("Mage");
   ((CheckBox)form.lookup(".check-box")).setSelected(tutorial);check(!begin.isDisabled(),"Valid name can start");begin.fire();
  }catch(Throwable error){finish(error);}});
  click("title-new");
 }
 static VBox slot(int number){return root().lookupAll(".card").stream().filter(n->n instanceof VBox box&&box.getChildren().stream().anyMatch(c->c instanceof Label l&&l.getText().equals("Manual slot "+number))).map(n->(VBox)n).findFirst().orElseThrow();}
 static void click(String id){Node n=root().lookup("#"+id);if(!(n instanceof Button b))throw new AssertionError("Missing button #"+id);b.fire();}
 @SuppressWarnings("unchecked") static void category(String name){((ComboBox<String>)root().lookup("#collection-category")).setValue(name);}
 static void finish(Throwable error){try{if(app!=null)app.stop();if(stage!=null)stage.close();}catch(Throwable ignored){}if(error!=null){error.printStackTrace();System.exit(1);}System.out.println("PASS / "+checks+" UI checks: atlas, fog, inventory grid/context/discard, Codex collections, transaction actions, dialogue, save/recovery and accessibility.");System.exit(0);}
 public static void main(String[] args){
  String project=System.getProperty("teleknia.uiCheck.root");var cwd=java.nio.file.Path.of("").toAbsolutePath().normalize();
  if(project==null||!cwd.getParent().equals(java.nio.file.Path.of(project).resolve("target/ui-checks").toAbsolutePath().normalize()))throw new IllegalStateException("Run scripts/check-ui.ps1 in isolation");
  var watchdog=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});watchdog.schedule(()->{System.err.println("UI check timed out");System.exit(2);},100,java.util.concurrent.TimeUnit.SECONDS);
  Platform.startup(()->{try{
   app=new MainApp();stage=new Stage();app.start(stage);var field=MainApp.class.getDeclaredField("gameService");field.setAccessible(true);game=(GameService)field.get(app);app.getAudio().setMuted(true);
   check(stage.getTitle().contains("v"+com.rpgchatgame.util.Constants.APP_VERSION),"Release version in window");
   capture("campfire-title-empty-en");check(((Button)root().lookup("#title-continue")).isDisabled(),"No fabricated Continue without autosave");check(root().lookupAll(".slider").isEmpty(),"Start footer has no volume sliders");
   create("Aria",true);for(int i=0;i<4;i++)button("Next").fire();button("Finish").fire();check(app.getSettings().tutorialSeen(),"Tutorial completion");
   app.showHome();check(!((Button)root().lookup("#title-continue")).isDisabled(),"Continue enabled for valid autosave");check(label("Aria"),"Continue previews saved name without loading");capture("campfire-title-saved-en");
   app.showStory();click("header-menu");check(label("ADVENTURE")&&label("CHARACTER")&&label("SYSTEM"),"Menu grouped into three sections");
   check(root().lookup(".journey-reminder")!=null,"Pause menu shows next objective");key(KeyCode.ESCAPE);check(root().lookup("#scene-map")!=null,"Menu returns to gameplay instead of title");
   check(root().lookupAll(".text-input").isEmpty(),"No custom chat");check(root().lookup("#scene-map")!=null,"Dedicated minimap exists");
   check(root().lookupAll(".choice-button").stream().noneMatch(n->n instanceof Button b&&b.getText().startsWith("Travel")),"Travel removed from action cards");
   Platform.runLater(()->{try{var d=dialog();check(d.lookup("#map-node-tower")==null,"Remote nodes not exposed");Button gate=(Button)d.lookup("#map-node-gate");check(!gate.getAccessibleText().contains("Northern"),"Frontier hides name");gate.fire();}catch(Throwable e){finish(e);}});click("scene-map");
   check(game.getCurrentState().getCurrentLocation().equals("Northern Gate"),"Minimap frontier travel");game.performAction("fight_wolf");app.showStory();
   Platform.runLater(()->{try{var d=dialog();((Button)d.lookup("#map-node-village")).fire();check(game.getCurrentState().getCampaign().getEnemy()!=null,"Map cannot escape battle");button(d,"Close").fire();}catch(Throwable e){finish(e);}});click("scene-map");
   game.performAction("flee");game.getCurrentState().getCampaign().getFlags().addAll(List.of("survivors","wolf","guardian"));game.getCurrentState().getCharacter().setGold(500);game.performAction("look");
   new InventoryService().addItem(game.getCurrentState().getInventory(),ItemCatalog.stack("iron_sword",1));new InventoryService().addItem(game.getCurrentState().getInventory(),ItemCatalog.stack("ranger_armor",1));
   app.showInventory();check(root().lookupAll(".inventory-slot").size()==30,"Exactly thirty visible slots");
   check(((ComboBox<?>)root().lookup("#inventory-filter")).getItems().size()==2,"Only All and Quest Items categories");click("item-iron_sword");int hp=game.getCurrentState().getCharacter().getHp();
   check(game.getCurrentState().getCampaign().getWeapon().equals("rusty_sword"),"Selecting equipment is read-only");click("inventory-primary");check(game.getCurrentState().getCampaign().getWeapon().equals("iron_sword"),"Equip through detail");
   check(root().lookup("#equipped-weapon")!=null,"Paper doll equipment slot");Button iron=(Button)root().lookup("#item-iron_sword");check(iron.getContextMenu().getItems().getFirst().getText().equals("Unequip"),"Right-click toggles equipment action");
   check(iron.getTooltip().getShowDelay().toMillis()==1000,"One second hover detail");iron.getContextMenu().getItems().getFirst().fire();check(game.getCurrentState().getCampaign().getWeapon().isBlank(),"Unequip context action");
   check(game.getCurrentState().getCharacter().getHp()==hp,"Unequip does not heal");
   Platform.runLater(()->{try{button(dialog(),"Cancel").fire();}catch(Throwable e){finish(e);}});click("inventory-remove");check(ItemCatalog.owns(game.getCurrentState(),"iron_sword"),"Remove cancellation preserves item");
   Platform.runLater(()->{try{button(dialog(),"Remove").fire();}catch(Throwable e){finish(e);}});click("inventory-remove");check(!ItemCatalog.owns(game.getCurrentState(),"iron_sword"),"Confirmed remove discards item");
   ((ComboBox<String>)root().lookup("#inventory-filter")).setValue("Quest Items");click("item-old_map");check(((Button)root().lookup("#inventory-remove")).isDisabled(),"Quest remove protected in UI");
   for(var entry:WorldContent.read("items",ItemCatalog.Entry[].class))if(!ItemCatalog.owns(game.getCurrentState(),entry.item().getId()))new InventoryService().addItem(game.getCurrentState().getInventory(),ItemCatalog.stack(entry.item().getId(),1));
   ((ComboBox<String>)root().lookup("#inventory-filter")).setValue("All");check(!((Button)root().lookup("#inventory-next")).isDisabled(),"More than thirty items use pagination");capture("inventory-rpg-en");click("inventory-next");
   check(root().lookupAll(".inventory-slot").size()==30&&((Button)root().lookup("#inventory-next")).isDisabled(),"Last page keeps thirty slots and clamps next");click("inventory-prev");
   ((ComboBox<String>)root().lookup("#inventory-sort")).setValue("Quantity");click("item-small_potion");int old=BondService.quantity(game.getCurrentState(),"small_potion");
   Platform.runLater(()->{try{((Spinner<Integer>)dialog().lookup("#remove-quantity")).getValueFactory().setValue(2);button(dialog(),"Remove").fire();}catch(Throwable e){finish(e);}});click("inventory-remove");check(BondService.quantity(game.getCurrentState(),"small_potion")==old-2,"Stack discard quantity UI");
   app.showAdventure(2,"");click("entry-small_potion");int gold=game.getCurrentState().getCharacter().getGold();check(root().lookupAll(".codex-category").size()==10,"All nine Codex sections");
   check(game.getCurrentState().getCharacter().getGold()==gold,"Selecting market card never purchases");((Spinner<Integer>)root().lookup("#market-quantity")).getValueFactory().setValue(2);int price=WorldService.price(game.getCurrentState(),ItemCatalog.entry("small_potion"));click("buy-small_potion");check(game.getCurrentState().getCharacter().getGold()==gold-price*2,"Explicit atomic bulk purchase");check(((Label)root().lookup("#save-status")).getText().equals("Saved"),"Saved indicator follows actual commit");check(root().lookup("#codex-feedback").isVisible(),"Successful purchase produces a contextual transient notice");check(((Label)root().lookup("#context-controls")).getText().contains("Buy"),"Market footer is contextual");capture("codex-market-en");
   app.showAdventure(3,"");click("entry-mana_ward");click("upgrade-mana_ward");check(SkillCatalog.rank(game.getCurrentState(),"mana_ward")==1,"Skill detail upgrade");answerConfirm();click("reset-build");check(SkillCatalog.rank(game.getCurrentState(),"mana_ward")==0,"Build reset confirmation");
   app.showAdventure(1,"");click("entry-q_signal");click("track-q_signal");check(game.getCurrentState().getCampaign().getTrackedQuest().equals("q_signal"),"Quest selection and pin");capture("codex-journal-en");
   app.showAdventure(0,"");check(root().lookup("#map-node-glacier")==null,"Remote undiscovered nodes stay hidden");capture("atlas-fog-en");
   click("map-node-frostveil");check(game.getCurrentState().getCurrentLocation().equals("Frostveil Pass"),"Codex map shares travel rules");click("npc-sera");check(game.getCurrentState().getCampaign().getDialogueNode().equals("sera"),"NPC portrait interaction");
   Platform.runLater(()->{try{var d=dialog();((Button)d.lookup("#map-node-camp")).fire();check(game.getCurrentState().getCurrentLocation().equals("Frostveil Pass"),"Map cannot interrupt NPC conversation");button(d,"Close").fire();}catch(Throwable e){finish(e);}});click("scene-map");game.performAction("dialogue_close");
   app.showAdventure(4,"");category("Characters");click("entry-npc-sera");check(label("Sera, Mountain Scout"),"Archive known character detail");capture("codex-archive-en");
   category("Enemies");click("entry-enemy-rime_sentinel");check(!label("Rime Sentinel"),"Locked lore does not expose enemy name");
   game.travel("camp");game.getCurrentState().getCampaign().getFlags().addAll(List.of("q_signal","q_caravan","q_oath","q_medicine","q_chart"));game.performAction("look");
   app.showAdventure(6,"");click("entry-mira");click("recruit-mira");click("activate-mira");click("talk-mira");check(game.getCurrentState().getCampaign().has("journey_start_mira"),"Companion talk starts journey");
   game.travel("village");game.performAction("journey_mira");game.travel("camp");app.showAdventure(6,"");click("entry-mira");click("bond-mira");check(game.getCurrentState().getCampaign().has("bond_mira"),"Companion quest via detail");
   app.showAdventure(6,"");category("Crafting");click("entry-potion");int materials=BondService.quantity(game.getCurrentState(),"ember_shard");click("craft-potion");check(BondService.quantity(game.getCurrentState(),"ember_shard")==materials-1,"Crafting detail action");capture("codex-crafting-en");
   app.showAdventure(7,"");check(((Button)root().lookup("#challenge-ash")).isDisabled(),"Arena gated before ending");check(((Label)root().lookup("#arena-blocked-reason")).getText().contains("Complete the main journey"),"Arena displays a specific unlock reason");
   int arenaGold=game.getCurrentState().getCharacter().getGold();click("entry-ice");check(game.getCurrentState().getCharacter().getGold()==arenaGold&&root().lookup("#challenge-ice")!=null,"Selecting an arena never enters or charges");click("entry-ash");capture("arena-preparation-locked-en");
   game.getCurrentState().getCampaign().setEnding("THE LAST SEAL");game.performAction("look");game.performAction("explore_borderlands");app.showAdventure(7,"");capture("arena-preparation-ready-en");click("challenge-ash");check(game.getCurrentState().getCampaign().getEnemy().getId().equals("arena_ash"),"Explicit arena enter");capture("arena-rpg-en");game.performAction("flee");
   app.showAdventure(8,"");check(label("Ending / THE LAST SEAL"),"Ending gallery detail");category("Achievements");capture("codex-gallery-en");
   app.showAdventure(5,"");check(root().lookup("#collection-detail")!=null,"Chronicle read-only detail");
   long successfulRevision=game.saveRevision();String untouched=com.rpgchatgame.util.JsonUtil.toJson(game.getCurrentState());
   try(var c=new DatabaseManager().getConnection();var q=c.createStatement()){q.executeUpdate("CREATE TRIGGER fail_ui_save BEFORE UPDATE ON save_slots BEGIN SELECT RAISE(ABORT,'ui save check'); END");}
   Platform.runLater(()->{try{button(dialog(),"Close").fire();}catch(Throwable error){finish(error);}});app.runSafely(()->game.performAction("look"));
   check(((Label)root().lookup("#save-status")).getText().equals("Not saved"),"Failed save is explicitly marked, never reported Saved");check(game.saveRevision()==successfulRevision&&com.rpgchatgame.util.JsonUtil.toJson(game.getCurrentState()).equals(untouched),"Failure notice preserves current progression");
   try(var c=new DatabaseManager().getConnection();var q=c.createStatement()){q.executeUpdate("DROP TRIGGER fail_ui_save");}
   key(KeyCode.F5);button(slot(1),"Save here").fire();check(game.saveSlots().get(1).occupied(),"Manual checkpoint UI");
   game.loadSlot(1);try(var c=new DatabaseManager().getConnection();var q=c.createStatement()){q.executeUpdate("UPDATE save_slots SET payload='bad' WHERE slot=1");}app.showSaves(false,"");answerConfirm();button(slot(1),"Restore backup").fire();check(!game.saveSlots().get(1).corrupt(),"Save recovery UI");
   app.showSettings();((ComboBox<String>)root().lookup("#language-setting")).setValue("id");((ComboBox<Integer>)root().lookup("#text-size-setting")).setValue(120);((CheckBox)root().lookup("#reduced-motion-setting")).setSelected(true);app.getSettings().setReducedMotion(true);
   check(label("PENGATURAN"),"Bilingual settings");stage.setWidth(980);stage.setHeight(680);
   later(()->{try{
    app.showStory();capture("story-rpg-id-minimum");check(root().lookupAll(".choice-button").stream().noneMatch(n->n instanceof Button b&&b.getText().startsWith("Pergi")),"No localized travel cards");
    app.showHome();capture("campfire-title-id-minimum");app.showStory();app.showInventory();capture("inventory-rpg-id-minimum");check(root().lookupAll(".inventory-slot").size()==30,"Thirty inventory slots at minimum size");
    for(int i=0;i<9;i++){app.showAdventure(i,"");check(root().lookup("#codex-section-"+i)!=null,"Codex section accessible at minimum size: "+i);capture("codex-section-"+i+"-id-minimum");}
    app.showAdventure(0,"");capture("atlas-id-minimum");
    for(var l:WorldContent.LOCATIONS.values())game.getCurrentState().getCampaign().getVisited().add(l.name());app.showAdventure(0,"");root();
    var mapBoard=root().lookup("#map-board");var boardBounds=mapBoard.localToScene(mapBoard.getBoundsInLocal());
    for(Node n:root().lookupAll(".map-node")){var b=n.localToScene(n.getBoundsInLocal());check(b.getMinX()>=boardBounds.getMinX()-1&&b.getMaxX()<=boardBounds.getMaxX()+1&&b.getMinY()>=boardBounds.getMinY()-1&&b.getMaxY()<=boardBounds.getMaxY()+1,"Map node stays inside parchment: "+n.getId());}capture("atlas-complete-id-minimum");
    app.showAdventure(4,"");category("Locations");click("entry-location-village");category("Characters");click("entry-npc-sera");category("Locations");check(label("Burned Village"),"Collection remembers selection separately per category");
    app.showStory();var reader=new com.rpgchatgame.ui.DialoguePager("One remembered road. ".repeat(30),false);check(!reader.isFinished(),"Typewriter starts locked");reader.advance();int n=0;while(!reader.isFinished()&&n++<30)reader.advance();check(reader.isFinished(),"Click reveal/continue finishes pages");

    String readingProperty=System.getProperty("teleknia.uiCheck.root");System.clearProperty("teleknia.uiCheck.root");app.getSettings().setReducedMotion(false);
    String longNarration="A traveler remembers the winter.\n\n".repeat(35)+"A final promise. 🌙";game.getCurrentState().setCurrentStorySummary(longNarration);app.showStory();System.setProperty("teleknia.uiCheck.root",readingProperty);
    var liveReader=(com.rpgchatgame.ui.DialoguePager)root().lookup("#dialogue-reader");check(!liveReader.isFinished(),"Continuous narration starts animated");
    int logCount=game.getCurrentState().getStoryLogs().size();Button menu=(Button)root().lookup("#header-menu");
    for(var type:java.util.List.of(MouseEvent.MOUSE_PRESSED,MouseEvent.MOUSE_RELEASED,MouseEvent.MOUSE_CLICKED))Event.fireEvent(menu,new MouseEvent(type,4,4,4,4,MouseButton.PRIMARY,1,false,false,false,false,type==MouseEvent.MOUSE_PRESSED,false,false,false,false,false,null));
    check(liveReader.isFinished()&&liveReader.visibleText().equals(longNarration),"One anywhere click reveals the entire original text");check(root().lookup("#dialogue-reader")!=null,"Skip click does not activate Menu");check(game.getCurrentState().getStoryLogs().size()==logCount,"Skipping never commits a game action");
    check(!((Label)root().lookup("#dialogue-hint")).getText().contains("/"),"No page fraction in dialogue");root();var narrativeScroll=(ScrollPane)root().lookup("#narration-scroll");check(narrativeScroll.getContent().getLayoutBounds().getHeight()>narrativeScroll.getViewportBounds().getHeight(),"Long dialogue really scrolls");check(root().lookup(".top-nav").lookupAll(".button").size()==1,"Header exposes only Menu");capture("traveler-story-continuous-id");
    click("header-menu");check(root().lookup("#menu-inventory")!=null,"Inventory included in unified Menu");capture("traveler-menu-id");click("menu-inventory");check(root().lookup("#paper-doll")!=null,"Menu opens inventory");
    app.showAdventure(2,"");capture("traveler-market-id");check(!root().lookup("#action-toast").isVisible(),"Old notices clear on navigation");app.showStory();
    String checkProperty=System.getProperty("teleknia.uiCheck.root");System.clearProperty("teleknia.uiCheck.root");app.getSettings().setReducedMotion(false);
    Platform.runLater(()->{try{
        var sheet=dialog();check(sheet.lookup("#map-board").isDisabled(),"Travel blocked during unfold animation");
        check(sheet.lookup("#atlas-view").getScaleX()<1,"Map starts folded");
        var wait=new javafx.animation.PauseTransition(javafx.util.Duration.millis(620));wait.setOnFinished(e->{try{check(!sheet.lookup("#map-board").isDisabled()&&Math.abs(sheet.lookup("#atlas-view").getScaleX()-1)<.01,"Map unfolds and enables travel");button(sheet,com.rpgchatgame.util.UiText.tr("Close")).fire();}catch(Throwable error){finish(error);}});wait.play();
    }catch(Throwable error){finish(error);}});app.showMap();System.setProperty("teleknia.uiCheck.root",checkProperty);
    key(KeyCode.F11);check(stage.isFullScreen(),"Fullscreen shortcut");key(KeyCode.F11);
    finish(null);
   }catch(Throwable error){finish(error);}});
  }catch(Throwable error){finish(error);}});
 }
}
