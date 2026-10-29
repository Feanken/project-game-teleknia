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
 static void later(Runnable action){var pause=new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));pause.setOnFinished(e->{try{action.run();}catch(Throwable error){finish(error);}});pause.play();}
 static void capture(String file)throws Exception{
  root().applyCss();root().layout();
  for(var node:root().lookupAll(".chat-scroll"))if(node instanceof ScrollPane pane)pane.setVvalue(1);
  var image=stage.getScene().snapshot(null);var out=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
  for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++)out.setRGB(x,y,image.getPixelReader().getArgb(x,y));
  ImageIO.write(out,"png",new File(file+".png"));
 }
 static Parent dialog(){return Window.getWindows().stream().filter(w->w!=stage&&w.isShowing()).findFirst().orElseThrow().getScene().getRoot();}
 static void answerConfirm(){Platform.runLater(()->{try{button(dialog(),"Confirm").fire();}catch(Throwable error){finish(error);}});}
 @SuppressWarnings("unchecked") static void create(String name,boolean tutorial){
  Platform.runLater(()->{try{
   var form=dialog();form.applyCss();form.layout();TextField field=(TextField)form.lookup(".text-field");field.setText("");
   Button begin=button(form,"Begin adventure");check(begin.isDisabled(),"Blank name cannot start");field.setText(name);
   ((ComboBox<String>)form.lookup(".combo-box")).getSelectionModel().select("Mage");
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
   TextField input=(TextField)root().lookup(".custom-action");input.requestFocus();logs=game.getCurrentState().getStoryLogs().size();key(KeyCode.DIGIT2);
   check(game.getCurrentState().getStoryLogs().size()==logs,"Typing does not trigger choices");
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
    check(label("PENGELOLA SAVE"),"Save manager localized");finish(null);
   }catch(Throwable error){finish(error);}});
  }catch(Throwable error){finish(error);}});
 }
}

