package com.rpgchatgame.ui;

import com.rpgchatgame.MainApp;
import com.rpgchatgame.service.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.geometry.*;
import javafx.animation.*;
import javafx.util.Duration;
import com.rpgchatgame.util.UiText;
import java.util.*;

/** One fog-safe graph shared by the minimap overlay and Codex. */
public final class AtlasView extends BorderPane {
    private static final Map<String,double[]> POINTS=Map.ofEntries(
        Map.entry("village",new double[]{.10,.48}),Map.entry("gate",new double[]{.26,.28}),Map.entry("camp",new double[]{.38,.64}),
        Map.entry("trail",new double[]{.53,.40}),Map.entry("shrine",new double[]{.70,.17}),Map.entry("ruins",new double[]{.74,.69}),
        Map.entry("tower",new double[]{.96,.43}),Map.entry("frostveil",new double[]{.04,.04}),Map.entry("glacier",new double[]{.41,.03}),
        Map.entry("coast",new double[]{.07,.94}),Map.entry("lighthouse",new double[]{.44,.95}));
    private final Pane board=new Pane();
    private final Canvas ink=new Canvas();
    private final MainApp app;
    private boolean traveling;
    public AtlasView(MainApp app,GameService game,Runnable arrived){
        this.app=app;getStyleClass().add("atlas-paper");setId("atlas-view");setPadding(new Insets(16));
        Label heading=new Label(UiText.tr("WORLD MAP"));heading.getStyleClass().add("atlas-title");
        Label hint=new Label(UiText.tr("Gold: current / Light: visited / Dark: unexplored"));hint.setWrapText(true);hint.getStyleClass().add("ink-label");
        Label status=new Label(UiText.tr("Select a connected location to travel. Unknown routes stay hidden."));status.setWrapText(true);status.setId("atlas-status");status.getStyleClass().add("ink-label");
        setTop(new VBox(6,heading,hint));setBottom(status);BorderPane.setMargin(status,new Insets(8,0,0,0));
        board.setId("map-board");board.setMinSize(500,260);board.setPrefSize(790,490);ink.setMouseTransparent(true);board.getChildren().add(ink);
        var state=game.getCurrentState();var nodes=AtlasService.nodes(state);Map<String,Button> buttons=new LinkedHashMap<>();
        for(var n:nodes){
            var l=WorldContent.LOCATIONS.get(n.id());Label name=new Label(n.known()?l.name():UiText.tr(n.reachable()?"Explore":"Unknown"));
            name.setMaxWidth(92);name.setPrefWidth(92);name.setAlignment(Pos.CENTER);name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);name.getStyleClass().add("map-node-label");
            Node art;if(n.known()){var image=PixelAssets.view(l.sprite());image.setClip(new Polygon(32,0,64,16,64,48,32,64,0,48,0,16));art=image;}else{Label fog=new Label("?");fog.getStyleClass().add("fog-symbol");art=fog;}
            VBox graphic=new VBox(2,art,name);graphic.setAlignment(Pos.CENTER);
            Button b=new Button();b.setGraphic(graphic);b.setAlignment(Pos.CENTER);b.setShape(new Polygon(28,0,84,0,112,53,84,106,28,106,0,53));b.setScaleShape(true);b.setPrefSize(112,106);b.setMinSize(112,106);b.setMaxSize(112,106);b.setId("map-node-"+n.id());
            b.getStyleClass().addAll("map-node",n.current()?"map-current":n.known()?"map-known":"map-fog");
            b.setAccessibleText(n.known()?l.name():UiText.tr("Unexplored location"));
            Tooltip tip=new Tooltip((n.known()?l.name()+"\n":"")+(n.current()?UiText.tr("You are here"):n.reachable()?UiText.tr("Travel"):UiText.tr("No open route. Finish combat or conversation, or continue the story.")));b.setTooltip(tip);
            b.setOnAction(e->app.runSafely(()->{
                if(traveling)return;
                if(!AtlasService.canTravel(game.getCurrentState(),n.id())){status.setText(tip.getText());return;}
                traveling=true;board.setDisable(true);
                try{game.travel(n.id());arrived.run();}catch(RuntimeException error){traveling=false;board.setDisable(false);throw error;}
            }));buttons.put(n.id(),b);board.getChildren().add(b);
        }
        Runnable layout=()->{
            double w=board.getWidth(),h=board.getHeight();boolean compact=h<410||w<700;double bw=compact?92:112,bh=compact?74:106;
            for(var b:buttons.values()){
                b.setMinSize(bw,bh);b.setPrefSize(bw,bh);b.setMaxSize(bw,bh);b.resize(bw,bh);
                var art=((VBox)b.getGraphic()).getChildren().getFirst();
                if(art instanceof javafx.scene.image.ImageView image){double size=compact?32:64;image.setFitWidth(size);image.setFitHeight(size);image.setClip(new Polygon(size/2,0,size,size/4,size,size*.75,size/2,size,0,size*.75,0,size/4));}
                else if(art instanceof Label fog)fog.setStyle(compact?"-fx-font-size: 22px; -fx-padding: 0;":"");
            }
ink.setWidth(w);ink.setHeight(h);var g=ink.getGraphicsContext2D();g.clearRect(0,0,w,h);
            g.setStroke(Color.web("#b8955a",.30));g.setLineWidth(1);
            for(int i=0;i<18;i++){double y=(i*43+19)%Math.max(1,h);g.beginPath();g.moveTo(0,y);g.bezierCurveTo(w*.3,y-18,w*.7,y+24,w,y-9);g.stroke();}
            Map<String,double[]> xy=new HashMap<>();for(var n:nodes){var p=POINTS.get(n.id());double x=bw/2+5+p[0]*(w-bw-10),y=bh/2+5+p[1]*(h-bh-10);xy.put(n.id(),new double[]{x,y});buttons.get(n.id()).relocate(x-bw/2,y-bh/2);}
            Set<String> done=new HashSet<>();g.setStroke(Color.web("#7e5934"));g.setLineWidth(2);
            for(var a:nodes)for(var r:WorldContent.LOCATIONS.get(a.id()).routes()){
                var b=nodes.stream().filter(n->n.id().equals(r.to())).findFirst().orElse(null);if(b==null||!a.known()||!b.known()&&!state.getCampaign().getFlags().containsAll(r.requires()))continue;
                String key=a.id().compareTo(b.id())<0?a.id()+b.id():b.id()+a.id();if(!done.add(key))continue;
                var x=xy.get(a.id());var y=xy.get(b.id());g.setLineDashes(b.known()?new double[]{}:new double[]{5,5});g.strokeLine(x[0],x[1],y[0],y[1]);
            }
            g.setLineDashes();g.setFill(Color.web("#755532"));g.fillText("N",w-28,18);g.strokeLine(w-25,24,w-25,46);g.strokeLine(w-30,30,w-25,24);g.strokeLine(w-20,30,w-25,24);
        };
        board.widthProperty().addListener((o,a,b)->layout.run());board.heightProperty().addListener((o,a,b)->layout.run());
        ScrollPane scroll=new ScrollPane(board);scroll.setFitToWidth(true);scroll.setFitToHeight(true);scroll.getStyleClass().add("plain-scroll");setCenter(scroll);
    }
    public void unfold(){
        if(app.getSettings().reducedMotion()||System.getProperty("teleknia.uiCheck.root")!=null)return;
        board.setDisable(true);setScaleX(.06);setOpacity(.2);
        ScaleTransition open=new ScaleTransition(Duration.millis(420),this);open.setToX(1);open.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade=new FadeTransition(Duration.millis(260),this);fade.setToValue(1);
        ParallelTransition animation=new ParallelTransition(open,fade);animation.setOnFinished(e->board.setDisable(false));
        sceneProperty().addListener((o,a,b)->{if(b==null)animation.stop();});animation.play();
    }
}
