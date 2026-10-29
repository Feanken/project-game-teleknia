package com.rpgchatgame.ui;

import com.rpgchatgame.util.UiText;
import javafx.scene.*;
import javafx.scene.control.*;
import java.util.Map;
import java.util.function.Consumer;

/** Reversible localization for UI controls; story messages and user text are left untouched. */
public final class UiLocalization {
    private UiLocalization() {}
    private static void text(Map<Object,Object> properties,String key,String current,Consumer<String> setter){
        if(current==null)return;
        String previous=(String)properties.get(key+".last");
        String source=current.equals(previous)?(String)properties.get(key+".source"):current;
        String translated=UiText.tr(source);
        properties.put(key+".source",source);properties.put(key+".last",translated);setter.accept(translated);
    }
    public static void apply(Node node){
        if(Boolean.TRUE.equals(node.getProperties().get("literal")))return;
        if(node.getAccessibleText()!=null)text(node.getProperties(),"ui.accessible",node.getAccessibleText(),node::setAccessibleText);
        if(node instanceof Labeled label)text(node.getProperties(),"ui.text",label.getText(),label::setText);
        if(node instanceof TextInputControl input)text(node.getProperties(),"ui.prompt",input.getPromptText(),input::setPromptText);
        if(node instanceof Control control&&control.getTooltip()!=null){var tooltip=control.getTooltip();text(tooltip.getProperties(),"ui.tip",tooltip.getText(),tooltip::setText);}
        if(node instanceof ScrollPane scroll&&scroll.getContent()!=null)apply(scroll.getContent());
        if(node instanceof DialogPane dialog&&dialog.getContent()!=null)apply(dialog.getContent());
        if(node instanceof TabPane tabs)for(Tab tab:tabs.getTabs()){
            text(tab.getProperties(),"ui.tab",tab.getText(),tab::setText);if(tab.getContent()!=null)apply(tab.getContent());
        }
        if(node instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable())apply(child);
    }
}
