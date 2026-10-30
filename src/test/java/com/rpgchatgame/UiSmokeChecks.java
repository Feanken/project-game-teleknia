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
  button("Start New Story").fire();
 }
 static VBox slot(int number){return root().lookupAll(".card").stream().filter(n->n instanceof VBox box&&box.getChildren().stream().anyMatch(c->c instanceof Label l&&l.getText().equals("Manual slot "+number))).map(n->(VBox)n).findFirst().orElseThrow();}
 static void finish(Throwable error){try{if(app!=null)app.stop();if(stage!=null)stage.close();}catch(Throwable ignored){}if(error!=null){error.printStackTrace();System.exit(1);}System.out.println("PASS / "+checks+" UI checks: tutorial, manual save/load/recovery, filtering, equipment preview, bilingual settings, keyboard, fullscreen, minimum window.");System.exit(0);}
 public static void main(String[] args){
  String project=System.getProperty("teleknia.uiCheck.root");
  java.nio.file.Path cwd=java.nio.file.Path.of("").toAbsolutePath().normalize();
  if(project==null||!cwd.getParent().equals(java.nio.file.Path.of(project).resolve("target/ui-checks").toAbsolutePath().normalize()))
   throw new IllegalStateException("Run scripts/check-ui.ps1; UI checks require an isolated target/ui-checks directory");
  var watchdog=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
  watchdog.schedule(()->{System.err.println("UI check timed out");System.exit(2);},55,java.util.concurrent.TimeUnit.SECONDS);
  Platform.startup(()->{try{
   app=new MainApp();stage=new Stage();app.start(stage);var field=MainApp.class.getDeclaredField("gameService");field.setAccessible(true);game=(GameService)field.get(app);app.getAudio().setMuted(true);
   check(stage.getTitle().contains("v"+com.rpgchatgame.util.Constants.APP_VERSION),"Window version matches game version");
   create("Aria",true);check(label("TRAVELER'S GUIDE"),"New game opens tutorial");
   for(int i=0;i<4;i++)button("Next").fire();button("Finish").fire();
   check(app.getSettings().tutorialSeen(),"Tutorial completion saved");check(root().lookup(".choices-box")!=null,"Tutorial returns to story");
   key(KeyCode.F1);check(label("TRAVELER'S GUIDE"),"F1 opens help");button("Skip tutorial").fire();
   int logs=game.getCurrentState().getStoryLogs().size();key(KeyCode.F5);check(label("SAVE MANAGER"),"F5 opens saves");
   button(slot(1),"Save here").fire();check(game.saveSlots().get(1).occupied(),"Manual checkpoint saved through UI");
   app.showHome();create("Bram",false);check(game.getCurrentState().getCharacter().getName().equals("Bram"),"New campaign only replaces auto");
   key(KeyCode.F9);answerConfirm();button(slot(1),"Load this save").fire();check(game.getCurrentState().getCharacter().getName().equals("Aria"),"Load checkpoint through UI");
   try(var c=new DatabaseManager().getConnection();var s=c.createStatement()){s.executeUpdate("UPDATE save_slots SET payload='bad' WHERE slot=1");}
   app.showSaves(false,"");check(label("Damaged save"),"Damage visible without default overwrite");
   answerConfirm();button(slot(1),"Restore backup").fire();check(!game.saveSlots().get(1).corrupt(),"Recovery through UI");
   app.showSaves(false,"");capture("saves-en");app.back();
   var actions=(Parent)root().lookup(".choices-box");actions.getChildrenUnmodifiable().getFirst().requestFocus();logs=game.getCurrentState().getStoryLogs().size();key(KeyCode.DIGIT1);
   check(game.getCurrentState().getStoryLogs().size()>logs,"Digit selects story action");
   check(root().lookupAll(".text-input").isEmpty()&&root().lookup(".form-field")==null,"No custom chat or text input in RPG field");
   app.showInventory();((ComboBox<String>)root().lookup("#inventory-filter")).getSelectionModel().select("ARMOR");check(label("No matching items"),"Empty inventory filter");
   ((ComboBox<String>)root().lookup("#inventory-filter")).getSelectionModel().select("WEAPON");check(label("Attack: 9 -> 9 (0)"),"Equipment comparison visible");
   capture("inventory-en");
   app.showSettings();app.showHelp();app.back();check(label("PREFERENCES"),"Help returns to settings");app.back();check(root().lookup("#inventory-filter")!=null,"Settings back returns to inventory without cycle");
   app.showHelp();app.showSettings();app.showSaves(false,"");app.showHelp();
   app.back();check(label("SAVE MANAGER"),"Nested back returns to saves");
   app.back();check(label("PREFERENCES"),"Nested back returns to settings");
   app.back();check(label("TRAVELER'S GUIDE"),"Nested back returns to guide");
   app.back();check(root().lookup("#inventory-filter")!=null,"Nested back returns to inventory without loop");
   app.showSettings();((ComboBox<String>)root().lookup("#language-setting")).getSelectionModel().select("id");check(label("PENGATURAN"),"Settings translated immediately");
   ((ComboBox<Integer>)root().lookup("#text-size-setting")).getSelectionModel().select(Integer.valueOf(120));
   check(app.getSettings().textScale()==1.2,"Text scale updated");capture("settings-id");
   app.showHelp();check(label("PANDUAN PETUALANG"),"Tutorial Indonesian");capture("tutorial-id");button("Lewati tutorial").fire();
   app.showStory();check(button("Dunia")!=null,"Story navigation Indonesian");
   key(KeyCode.F11);check(app.getSettings().fullscreen(),"F11 fullscreen");key(KeyCode.ESCAPE);check(!app.getSettings().fullscreen(),"Esc exits fullscreen");
   stage.setWidth(980);stage.setHeight(680);
   later(()->{try{
    game.performAction("go_gate");game.performAction("fight_wolf");app.showStory();capture("combat-id-120-minimum");
    check(stage.getScene().getWidth()<1000,"Minimum width applied");
    check(button("Serang")!=null,"Combat choices localized");
    app.showInventory();capture("inventory-id-120-minimum");
    app.showSaves(false,"");capture("saves-id-120-minimum");
    check(label("PENGELOLA SAVE"),"Save manager localized");
    game.performAction("flee");app.showAdventure(3,"");
    check(label("SKILL & BUILD"),"Build tab localized");
    ((Button)root().lookup("#upgrade-mana_ward")).fire();
    check(SkillCatalog.rank(game.getCurrentState(),"mana_ward")==1&&SkillCatalog.points(game.getCurrentState())==0,"Upgrade through build UI");
    answerConfirm();button("Reset build / 20 gold").fire();
    check(SkillCatalog.points(game.getCurrentState())==1,"Reset through build UI");
    ((ComboBox<String>)root().lookup("#build-difficulty")).getSelectionModel().select("STORY");
    check(CombatService.storyMode(game.getCurrentState()),"Difficulty selection at camp");capture("build-id-120-minimum");
    game.performAction("go_gate");game.performAction("fight_wolf");
    game.getCurrentState().getCampaign().setEnemy(new com.rpgchatgame.model.Enemy("wolf","Ash Wolf",200,14,2));app.showStory();
    button("Mana Ward / 7 MP").fire();
    check(game.getCurrentState().getCampaign().getPlayerEffects().containsKey("shield"),"Shield applied through UI");
    check(button("Mana Ward / 7 MP / CD 3").isDisabled(),"Cooldown disabled button shown");capture("ward-cooldown-id");
    button("Ember Surge / 6 MP").fire();
    check(game.getCurrentState().getCampaign().getEnemyEffects().containsKey("burn"),"Burn shown through UI");capture("burn-id");
    game.saveSlot(3);game.loadSlot(3);app.showStory();
    check(button("Mana Ward / 7 MP / CD 2").isDisabled(),"Loaded cooldown preserved in UI");
    ((Parent)root().lookup(".choices-box")).getChildrenUnmodifiable().getFirst().requestFocus();key(KeyCode.DIGIT0);
    check(game.getCurrentState().getCampaign().getEnemy()==null,"Zero hotkey selects tenth action retreat");
    game.getCurrentState().getCampaign().getFlags().addAll(java.util.List.of("wolf","guardian"));game.performAction("look");app.showStory();
    for(var node:root().lookupAll(".action-filter"))if(node instanceof ToggleButton toggle&&toggle.getText().equals(com.rpgchatgame.util.UiText.tr("Travel")))toggle.fire();
    button("Pergi ke Frostveil Pass").fire();check(game.getCurrentState().getCurrentLocation().equals("Frostveil Pass"),"Travel card opens expansion region");
    ((Button)root().lookup("#npc-sera")).fire();check(game.getCurrentState().getCampaign().getDialogueNode().equals("sera"),"Portrait opens NPC dialogue");
    capture("sera-dialogue-id");
    button("Di mana lensa sinyal berada?").fire();check(game.getCurrentState().getCampaign().getDialogueNode().equals("sera_signal"),"Dialogue question selects a branch");
    int logsBeforeRead=game.getCurrentState().getStoryLogs().size();Platform.runLater(()->{try{button(dialog(),"Tutup").fire();}catch(Throwable error){finish(error);}});
    button("Baca adegan").fire();check(game.getCurrentState().getStoryLogs().size()==logsBeforeRead,"Reading full narration spends no turn or action");
    button("Kembali ke pertanyaan awal").fire();button("Akhiri percakapan").fire();
    ((Button)root().lookup("#scene-map")).fire();check(label("Petualangan"),"Scenery opens map");
    app.showAdventure(4,"");check(label("Sera, Mountain Scout"),"Codex records discovered NPC");capture("codex-id");
    app.showAdventure(1,"");((Button)root().lookup("#track-q_signal")).fire();app.showStory();
    check(label("A Light Through Snow"),"Pinned quest appears in field HUD");capture("frostveil-field-id");
    game.performAction("go_glacier");game.performAction("fight_rime");app.showStory();
    int rimeHp=game.getCurrentState().getCampaign().getEnemy().getHp();((Button)root().lookup("#scene-opponent")).fire();
    check(game.getCurrentState().getCampaign().getEnemy().getHp()<rimeHp,"Enemy portrait performs attack");
    var vitality=root().lookup("#enemy-vitality");var frame=root().lookup(".rpg-scene");
    check(vitality.localToScene(vitality.getBoundsInLocal()).getMaxY()<=frame.localToScene(frame.getBoundsInLocal()).getMaxY()+1,"Enemy HP remains inside scene frame at minimum size");
    check(root().lookupAll(".text-input").isEmpty(),"No text input after NPC or battle transitions");capture("rime-battle-id");
    game.performAction("flee");game.getCurrentState().getCampaign().getFlags().addAll(java.util.List.of("survivors","q_signal","q_caravan","q_oath","q_medicine","q_chart"));game.performAction("look");
    app.showAdventure(6,"");check(root().lookup("#recruit-mira")!=null,"Camp systems tab rendered");
    ((Button)root().lookup("#recruit-mira")).fire();((Button)root().lookup("#activate-mira")).fire();
    game.companion("talk","mira");game.performAction("go_village");game.performAction("journey_mira");game.performAction("go_camp");app.showAdventure(6,"");((Button)root().lookup("#bond-mira")).fire();
    check(game.getCurrentState().getCampaign().getCompanion().equals("mira")&&game.getCurrentState().getCampaign().has("bond_mira"),"Recruit, activate and personal quest through UI");
    app.showAdventure(6,"");int beforeCraft=com.rpgchatgame.service.BondService.quantity(game.getCurrentState(),"ember_shard");((Button)root().lookup("#craft-potion")).fire();
    check(com.rpgchatgame.service.BondService.quantity(game.getCurrentState(),"ember_shard")==beforeCraft-1,"Crafting consumes materials through UI");capture("bonds-crafting-id");
    app.showAdventure(7,"");check(((Button)root().lookup("#challenge-ash")).isDisabled(),"Arena gated before ending");
    game.getCurrentState().getCharacter().setGold(100);game.getCurrentState().getCampaign().setEnding("THE SEALED CROWN");game.getCurrentState().getCampaign().mark("borderlands_reopened","");game.performAction("look");
    app.showAdventure(7,"");capture("challenge-board-id");((Button)root().lookup("#challenge-ash")).fire();
    check(game.getCurrentState().getCampaign().getEnemy().getId().equals("arena_ash"),"Arena button opens encounter scene");capture("arena-battle-id");game.performAction("flee");
    app.showAdventure(8,"");check(label("Ending / THE SEALED CROWN"),"Cross-save ending gallery visible");capture("gallery-id");
    app.showSettings();((CheckBox)root().lookup("#reduced-motion-setting")).fire();check(app.getSettings().reducedMotion(),"Reduced motion saved through UI");
    var reader=new com.rpgchatgame.ui.DialoguePager("A traveler remembers the road. ".repeat(30)+"The end.",false);
    check(!reader.isFinished()&&reader.visibleText().isEmpty(),"Typewriter starts unrevealed");
    int readerLogs=game.getCurrentState().getStoryLogs().size();reader.advance();check(!reader.isFinished()&&reader.visibleText().length()>0,"Click reveals current page without skipping remaining pages");
    int steps=0;while(!reader.isFinished()&&steps++<30)reader.advance();check(reader.isFinished()&&steps>1,"Page continuation reaches final response gate");
    check(game.getCurrentState().getStoryLogs().size()==readerLogs,"Reading never dispatches game actions");
    var instantReader=new com.rpgchatgame.ui.DialoguePager("Full text immediately",true);check(instantReader.isFinished()&&instantReader.visibleText().equals("Full text immediately"),"Reduced motion shows complete narration");
    app.getSettings().setReducedMotion(false);game.performAction("npc_mira");game.performAction("context_mira1");
    String smokeProperty=System.getProperty("teleknia.uiCheck.root");System.clearProperty("teleknia.uiCheck.root");app.showStory();System.setProperty("teleknia.uiCheck.root",smokeProperty);
    var liveReader=(com.rpgchatgame.ui.DialoguePager)root().lookup("#dialogue-reader");
    check(root().lookup(".choices-box").isDisabled(),"Actual story choices locked while text is unread");liveReader.advance();capture("context-dialogue-16");
    int readingSteps=0;while(!liveReader.isFinished()&&readingSteps++<20)liveReader.advance();
    check(!root().lookup(".choices-box").isDisabled(),"Actual choices unlock after final page");
    finish(null);
   }catch(Throwable error){finish(error);}});
  }catch(Throwable error){finish(error);}});
 }
}

