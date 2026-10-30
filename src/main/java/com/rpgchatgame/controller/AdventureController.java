package com.rpgchatgame.controller;
import com.rpgchatgame.MainApp;
import com.rpgchatgame.model.*;
import com.rpgchatgame.service.*;
import com.rpgchatgame.ui.*;
import com.rpgchatgame.util.UiText;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import java.util.*;
import java.util.function.Supplier;
import static com.rpgchatgame.ui.RpgUi.*;

/** Codex hub: independent categories, collection selection, explicit detail actions. */
public final class AdventureController {
    private final MainApp app;private final GameService game;private final int section;
    private final GameState state;
    private static final String[] TITLES={"WORLD MAP","QUEST JOURNAL","CAMP MARKET","SKILLS & BUILD","ARCHIVE","CHRONICLE","CAMP & BONDS","CHALLENGES","GALLERY"};
    private static final String[] ICONS={"old_map","journal","backpack","experience","rune_tablet","journal","ilyra","field_guardian","star_chart"};
    public AdventureController(MainApp app,GameService game){this(app,game,0);}
    public AdventureController(MainApp app,GameService game,int tab){this.app=app;this.game=game;this.section=Math.max(0,Math.min(8,tab));state=game.getCurrentState();}
    private void refresh(String message){app.showAdventure(section,message);}
    private void transaction(Supplier<String> action){refresh(action.get());}
    private String milestone(String id){var q=WorldContent.QUESTS.get(id);return q==null?id:q.title();}
    private String blockedReason(String id,String fallback){
        var w=state.getCampaign();int gold=state.getCharacter().getGold();
        if(id.startsWith("track-"))return "This quest is already complete.";
        if(w.getEnemy()!=null)return "Finish the current battle";
        if(w.epilogueClosed())return "Reopen the borderlands";
        if(id.startsWith("buy-")){String item=id.substring(4);if(!game.canShop())return "Return to Survivor Camp";if(ItemCatalog.entry(item).item().getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(state,item))return "You already own this equipment.";return UiText.format("Need {0} gold (you have {1})",WorldService.price(state,ItemCatalog.entry(item)),gold);}
        if(id.startsWith("upgrade-"))return SkillCatalog.rank(state,id.substring(8))>=3?"Maximum rank reached.":"Level up to earn another skill point.";
        if(id.equals("reset-build")){if(!game.canShop())return "Return to Survivor Camp";return w.getSkillRanks().isEmpty()?"No allocated points to reset.":UiText.format("Need {0} gold (you have {1})",20,gold);}
        if(id.startsWith("craft-")){
            var r=BondService.RECIPES.stream().filter(recipe->id.equals("craft-"+recipe.id())).findFirst().orElseThrow();
            if(!w.getDialogueNode().isBlank())return "Finish the current conversation";if(!BondService.camp(state))return "Return to Survivor Camp";
            if(!w.has(r.blueprint()))return UiText.format("Complete {0} to unlock this recipe.",milestone(r.blueprint()));
            int amount=BondService.quantity(state,r.material());if(amount<r.amount())return UiText.format("Need {0} x{1} (you have {2})",ItemCatalog.equippedName(r.material()),r.amount(),amount);
            if(gold<r.gold())return UiText.format("Need {0} gold (you have {1})",r.gold(),gold);return "You already own this equipment.";
        }
        if(id.startsWith("recruit-")||id.startsWith("activate-")||id.startsWith("talk-")||id.startsWith("bond-")){
            var c=BondService.companion(id.substring(id.indexOf('-')+1));if(!w.getDialogueNode().isBlank())return "Finish the current conversation";if(!BondService.camp(state))return "Return to Survivor Camp";
            if(id.startsWith("recruit-"))return BondService.recruited(state,c)?"Already recruited.":UiText.format("Complete {0} first.",milestone(c.gate()));
            if(!BondService.recruited(state,c))return "Recruit this companion first.";if(w.has("bond_"+c.id()))return "Personal quest already completed.";
            return LivingService.next(state,c);
        }
        return fallback;
    }
    private Node action(String id,String label,boolean enabled,String reason,Runnable run){Button b=button(app,label,run);b.setId(id);b.setDisable(!enabled);String explanation=enabled?"":blockedReason(id,reason);b.setTooltip(tip(explanation));Label why=text(explanation);why.getStyleClass().add("action-reason");return new VBox(5,b,why);}

    private VBox panel(String sprite,String title,String subtitle,Node... extra){VBox v=new VBox(12,new HBox(12,RpgUi.icon(sprite),new VBox(6,title(title),text(subtitle))));v.getChildren().addAll(extra);return v;}
    private CollectionView.Entry entry(String id,String title,String sprite,String summary,Supplier<Node> detail){return new CollectionView.Entry(id,title,sprite,summary,detail);}
    private Map<String,List<CollectionView.Entry>> groups(String category,List<CollectionView.Entry> entries){Map<String,List<CollectionView.Entry>> m=new LinkedHashMap<>();m.put(category,entries);return m;}
    public Parent createView(){
        BorderPane root=new BorderPane();root.getStyleClass().addAll("app-root","parchment-shell");root.setTop(nav(app,TITLES[section]));
        VBox categories=new VBox(6);categories.setPadding(new Insets(12,6,12,12));categories.setPrefWidth(166);categories.setMinWidth(146);categories.setMaxWidth(182);
        for(int i=0;i<TITLES.length;i++){final int index=i;Button b=button(app,TITLES[i],()->app.showAdventure(index,""));b.setId("codex-section-"+i);b.setMaxWidth(Double.MAX_VALUE);b.setWrapText(true);b.getStyleClass().add("codex-category");if(i==section)b.getStyleClass().add("category-selected");categories.getChildren().add(b);}
        Button bag=button(app,"Inventory",app::showInventory);bag.setId("codex-inventory");bag.getStyleClass().add("codex-category");bag.setMaxWidth(Double.MAX_VALUE);categories.getChildren().addFirst(bag);ScrollPane nav=scroll(categories);nav.setPrefWidth(180);nav.setMinWidth(158);root.setLeft(nav);app.remember("codex.section",Integer.toString(section));
        Node content=switch(section){case 0->new AtlasView(app,game,app::showStory);case 1->journal();case 2->market();case 3->build();case 4->archive();case 5->chronicle();case 6->bonds();case 7->challenges();default->gallery();};
        Label title=title(TITLES[section]);title.getStyleClass().add("codex-section-title");title.setPadding(new Insets(12,14,0,14));title.setText("");Label feedback=text("");feedback.getStyleClass().add("feedback-label");feedback.setId("codex-feedback");
        VBox center=new VBox(8,title,content);center.setMinHeight(0);center.setPadding(new Insets(0,10,10,0));VBox.setVgrow(content,Priority.ALWAYS);root.setCenter(center);return root;
    }
    private Node view(Map<String,List<CollectionView.Entry>> groups){return new CollectionView(app,"codex."+section,groups);}
    private Node journal(){
        Map<String,List<CollectionView.Entry>> lists=groups("Active",new ArrayList<>());lists.put("Completed",new ArrayList<>());
        for(var q:WorldContent.QUESTS.values()){boolean done=state.getCampaign().has(q.id());lists.get(done?"Completed":"Active").add(entry(q.id(),q.title(),"journal",q.region(),()->panel("journal",q.title(),q.region(),text(q.description()),title("Next objective"),text(LivingService.questNext(state,q.id())),text(state.getCampaign().getTrackedQuest().equals(q.id())?"TRACKED QUEST":""),action("track-"+q.id(),"Track quest",!done,"Completed quests cannot be tracked.",()->{game.trackQuest(q.id());refresh("");}))));}
        for(var c:BondService.COMPANIONS){boolean done=state.getCampaign().has("journey_end_"+c.id());lists.get(done?"Completed":"Active").add(entry("journey-"+c.id(),c.quest(),c.sprite(),LivingService.next(state,c),()->panel(c.sprite(),c.quest(),c.name(),text(LivingService.next(state,c)),text("Relevant milestones: "+String.join(", ",c.bonds())))));}
        return view(lists);
    }
    private Node market(){
        List<CollectionView.Entry> rows=new ArrayList<>();
        for(var e:ItemCatalog.shop()){var item=e.item();int price=WorldService.price(state,e);rows.add(entry(item.getId(),item.getName(),PixelAssets.item(item.getId()),price+" gold",()->{
            boolean owned=item.getType()!=Item.ItemType.CONSUMABLE&&ItemCatalog.owns(state,item.getId());Spinner<Integer> amount=new Spinner<>(1,item.getType()==Item.ItemType.CONSUMABLE?Math.max(1,Math.min(99,state.getCharacter().getGold()/Math.max(1,price))):1,1);amount.setId("market-quantity");amount.setEditable(false);
            // Purchases remain one transaction per item; quantity is an explicit atomic service call.
            return panel(PixelAssets.item(item.getId()),item.getName(),UiText.tr(item.getRarity().name()),text(item.getDescription()),text(item.getEffect()),text(InventoryQuery.comparison(state,item.getId())),text("Gold: "+state.getCharacter().getGold()+" / Price: "+price),text(WorldService.reputation(state,"camp")>=3?"Trusted by Survivor Camp / 20% market discount":""),amount,action("buy-"+item.getId(),owned?"Owned":"Buy",game.canShop()&&!owned&&state.getCharacter().getGold()>=price,"Visit camp outside combat; check gold and ownership.",()->transaction(()->game.buyItems(item.getId(),amount.getValue()))),text(game.canShop()?"The smith and herbalist are ready to trade.":"Visit Survivor Camp outside combat to trade."));
        }));}return view(groups("Goods",rows));
    }
    private Node build(){
        List<CollectionView.Entry> rows=new ArrayList<>();
        for(var s:SkillCatalog.forClass(state.getCharacter().getCharacterClass()))rows.add(entry(s.id(),s.name(),"experience",s.mp()+" MP",()->{
            int rank=SkillCatalog.rank(state,s.id());ComboBox<String> difficulty=new ComboBox<>();difficulty.getItems().setAll("STANDARD","STORY");difficulty.setValue(state.getCampaign().getDifficulty());difficulty.setId("build-difficulty");difficulty.setDisable(!game.canShop());difficulty.setOnAction(e->app.runSafely(()->transaction(()->game.setDifficulty(difficulty.getValue()))));
            return panel("experience",s.name(),"Rank "+rank+" / 3",text(s.description()),text(s.mp()+" MP / Cooldown "+s.cooldown()),text("Current: "+SkillCatalog.preview(state,s,rank)),text(rank<3?"Next: "+SkillCatalog.preview(state,s,rank+1):"Maximum rank reached."),text("Skill points: "+SkillCatalog.points(state)),action("upgrade-"+s.id(),"Upgrade / 1 point",rank<3&&SkillCatalog.points(state)>0&&state.getCampaign().getEnemy()==null&&!state.getCampaign().epilogueClosed(),"Needs a skill point, rank below 3, and no combat.",()->transaction(()->game.upgradeSkill(s.id()))),action("reset-build","Reset build / 20 gold",game.canShop()&&!state.getCampaign().getSkillRanks().isEmpty()&&state.getCharacter().getGold()>=20,"Reset only at camp with 20 gold.",()->{if(app.confirm("Reset build?","Refund allocated points for 20 gold. HP, MP and character levels do not change."))transaction(game::resetBuild);}),title("Difficulty"),difficulty,text("Story: 35% less incoming damage. Standard: normal damage. Change at camp only. Guard recovers mana; fatigue prevents indefinite stalling."));
        }));return view(groups(state.getCharacter().getCharacterClass(),rows));
    }
    private Node archive(){
        Map<String,List<CollectionView.Entry>> lists=groups("Locations",new ArrayList<>());lists.put("Characters",new ArrayList<>());lists.put("Enemies",new ArrayList<>());
        for(var l:WorldContent.LOCATIONS.values())lists.get("Locations").add(lore("location:"+l.id(),l.name(),l.sprite(),l.description()));
        for(var n:WorldContent.NPCS.values())lists.get("Characters").add(lore("npc:"+n.id(),n.name(),n.sprite(),n.lore()));
        for(var e:WorldContent.MONSTERS.values())lists.get("Enemies").add(lore("enemy:"+e.id(),e.name(),e.sprite(),e.lore()));
        return view(lists);
    }
    private CollectionView.Entry lore(String id,String name,String sprite,String story){boolean known=state.getCampaign().getCodex().contains(id);return entry(id.replace(':','-'),known?name:"Undiscovered",known?sprite:"",known?"Discovered":"Explore to discover this entry.",()->panel(known?sprite:"",known?name:"Undiscovered",known?"Archive":"Locked",text(known?story:"Explore to discover this entry.")));}
    private Node chronicle(){
        List<CollectionView.Entry> rows=new ArrayList<>();var logs=state.getStoryLogs();for(int i=Math.max(0,logs.size()-150);i<logs.size();i++){var log=logs.get(i);String key="log-"+i;rows.add(entry(key,log.getSpeaker().name()+" / "+(i+1),"journal","Read history",()->{Label full=text(log.getMessage());full.getProperties().put("literal",true);return panel("journal","CHRONICLE",log.getSpeaker().name(),full);}));}Collections.reverse(rows);return view(groups("History",rows));
    }
    private Node bonds(){
        Map<String,List<CollectionView.Entry>> lists=groups("Companions",new ArrayList<>());lists.put("Crafting",new ArrayList<>());
        for(var c:BondService.COMPANIONS)lists.get("Companions").add(entry(c.id(),c.name(),c.sprite(),c.role(),()->{
            VBox p=panel(c.sprite(),c.name(),state.getCampaign().getCompanion().equals(c.id())?"ACTIVE":"",text(c.role()),text("Recruit: "+c.gate()),text("Affinity "+BondService.affinity(state,c)+" / 3"),title(c.quest()),text(LivingService.next(state,c)));
            boolean joined=BondService.recruited(state,c),camp=BondService.camp(state);
            for(String cmd:List.of("recruit","activate","talk","bond")){boolean available=camp&&(cmd.equals("recruit")?!joined&&state.getCampaign().has(c.gate()):joined)&&(!cmd.equals("bond")||BondService.affinity(state,c)>=2&&!state.getCampaign().has("bond_"+c.id())&&state.getCampaign().has("journey_field_"+c.id()));p.getChildren().add(action(cmd+"-"+c.id(),cmd,available,"Visit camp outside conversation/combat; complete the companion's milestones and field objective.",()->{String message=game.companion(cmd,c.id());if(cmd.equals("talk")||cmd.equals("bond"))app.showStory();else refresh(message);}));}
            p.getChildren().add(action("dismiss-companion","Rest companion at camp",camp&&!state.getCampaign().getCompanion().isBlank(),"Visit camp with an active companion.",()->transaction(()->game.companion("dismiss",""))));return p;
        }));
        for(var r:BondService.RECIPES){var item=ItemCatalog.entry(r.output()).item();lists.get("Crafting").add(entry(r.id(),item.getName(),PixelAssets.item(r.output()),r.gold()+" gold",()->panel(PixelAssets.item(r.output()),item.getName(),item.getEffect(),text("Blueprint: "+r.blueprint()),text(r.material()+": "+BondService.quantity(state,r.material())+" / "+r.amount()),text("Gold: "+state.getCharacter().getGold()+" / Cost: "+r.gold()),action("craft-"+r.id(),"Craft / "+r.gold()+" gold",BondService.camp(state)&&state.getCampaign().has(r.blueprint())&&BondService.quantity(state,r.material())>=r.amount()&&state.getCharacter().getGold()>=r.gold()&&(item.getType()==Item.ItemType.CONSUMABLE||!ItemCatalog.owns(state,r.output())),"Craft at camp with blueprint, gold and materials. Equipment cannot be duplicated.",()->transaction(()->game.craft(r.id()))))));}return view(lists);
    }
    private Node challenges(){return new ArenaView(app,game);}
    private Node gallery(){
        Map<String,List<CollectionView.Entry>> lists=groups("Endings",new ArrayList<>());lists.put("Achievements",new ArrayList<>());var unlocked=game.gallery();
        // Include actual archived ending names, including migrated saves, without leaking locked epilogues.
        int number=0;for(String id:unlocked)if(id.startsWith("Ending / ")){final String text=id;lists.get("Endings").add(entry("ending-"+number++,id.substring(9),"star_chart","Unlocked",()->panel("star_chart",text,"Profile collection",RpgUi.text(LocalStoryGeneratorService.ENDINGS.getOrDefault(text.substring(9),"Unlocked in a completed journey. Starting a new character does not erase this ending.")))));}
        while(number<3){int n=number++;lists.get("Endings").add(entry("ending-"+n,"Unknown ending","","Locked",()->panel("old_map","Unknown ending","Locked",text("Complete a journey to reveal this ending."))));}
        for(String name:List.of("A Kindness Remembered","Roads Beyond Emberwood","Found Family","Bonds and Embers","Master Artisan","Arena Champion")){boolean known=unlocked.contains("Achievement / "+name);lists.get("Achievements").add(entry(name,known?name:"Unknown achievement",known?"experience":"",known?"Unlocked":"Locked",()->panel(known?"experience":"",known?name:"Unknown achievement","Profile collection",text(known?"Unlocked across your adventures. Restoring an older slot does not erase this achievement.":"Continue exploring, helping companions, crafting and completing challenges."))));}return view(lists);
    }
}
