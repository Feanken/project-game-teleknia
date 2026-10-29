package com.rpgchatgame.service;

import java.nio.file.*;
import java.util.Properties;

/** Window/UI preferences are independent of character slots. */
public final class UiSettings {
    private final Path file;
    private String language="en";
    private double textScale=1;
    private boolean fullscreen;
    private boolean tutorialSeen;
    private double width=1180,height=820;
    public UiSettings(){this(Path.of("data","ui.properties"));}
    public UiSettings(Path file){this.file=file;load();}
    public String language(){return language;}
    public double textScale(){return textScale;}
    public boolean fullscreen(){return fullscreen;}
    public boolean tutorialSeen(){return tutorialSeen;}
    public double width(){return width;}
    public double height(){return height;}
    public void setLanguage(String value){if(!value.equals("en")&&!value.equals("id"))throw new IllegalArgumentException("Unsupported language");language=value;save();}
    public void setTextScale(double value){if(Double.isFinite(value)){textScale=Math.max(.9,Math.min(1.2,value));save();}}
    public void setFullscreen(boolean value){fullscreen=value;save();}
    public void setTutorialSeen(boolean value){tutorialSeen=value;save();}
    public void setWindowSize(double width,double height){if(Double.isFinite(width)&&Double.isFinite(height)){this.width=Math.max(980,Math.min(3840,width));this.height=Math.max(680,Math.min(2160,height));save();}}
    private double number(Properties p,String key,double fallback,double min,double max){try{double n=Double.parseDouble(p.getProperty(key));return Double.isFinite(n)?Math.max(min,Math.min(max,n)):fallback;}catch(Exception ignored){return fallback;}}
    private void load(){
        Properties p=new Properties();
        if(!Files.exists(file))return;
        try(var input=Files.newInputStream(file)){
            p.load(input);language=p.getProperty("language","en").equals("id")?"id":"en";
            textScale=number(p,"textScale",1,.9,1.2);width=number(p,"width",1180,980,3840);height=number(p,"height",820,680,2160);
            fullscreen=Boolean.parseBoolean(p.getProperty("fullscreen"));tutorialSeen=Boolean.parseBoolean(p.getProperty("tutorialSeen"));
        }catch(Exception error){System.getLogger(UiSettings.class.getName()).log(System.Logger.Level.WARNING,"Using default UI settings",error);}
    }
    public void save(){
        Properties p=new Properties();p.setProperty("language",language);p.setProperty("textScale",Double.toString(textScale));p.setProperty("fullscreen",Boolean.toString(fullscreen));
        p.setProperty("tutorialSeen",Boolean.toString(tutorialSeen));p.setProperty("width",Double.toString(width));p.setProperty("height",Double.toString(height));
        Path temporary=file.resolveSibling(file.getFileName()+".tmp");
        try{
            if(file.toAbsolutePath().getParent()!=null)Files.createDirectories(file.toAbsolutePath().getParent());
            try(var output=Files.newOutputStream(temporary)){p.store(output,"Teleknia UI settings");}
            try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException error){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        }catch(java.io.IOException error){throw new IllegalStateException("Unable to save UI preferences",error);}
    }
}
