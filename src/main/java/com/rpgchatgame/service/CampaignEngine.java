package com.rpgchatgame.service;

import com.rpgchatgame.model.*;
import com.rpgchatgame.util.JsonUtil;
import java.util.*;

/** UI/storage-independent rules. Evaluates a detached candidate; caller commits before publishing. */
public final class CampaignEngine {
    public record Transition(GameState state,StoryResponse response,String feedback) {}
    private final CharacterService characters=new CharacterService();
    private final InventoryService inventory=new InventoryService();
    private final LocalStoryGeneratorService story=new LocalStoryGeneratorService();

    public static void validateAction(String action){
        if(action==null||action.isBlank()||action.length()>250)throw new IllegalArgumentException("Enter an action of 1-250 characters");
    }
    public Transition advance(GameState source,String action) {
        validateAction(action);
        GameState next=JsonUtil.fromJson(JsonUtil.toJson(source),GameState.class);
        String label=next.getCurrentChoices().stream().filter(c->c.getAction().equals(action)).map(Choice::getLabel).findFirst().orElse(action);
        next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.PLAYER,label));
        StoryResponse response=story.generateNextScene(next,action);
        String levelMessage=characters.applyProgress(next.getCharacter(),response.getHpChange(),response.getMpChange(),response.getExpGain(),response.getGoldChange());
        for(var gained:response.getItemsGained())inventory.addItem(next.getInventory(),gained);
        if(response.getNewLocation()!=null)next.setCurrentLocation(response.getNewLocation());
        next.getCharacter().setCurrentLocation(next.getCurrentLocation());
        next.setCurrentQuest(story.quest(next));next.getCharacter().setCurrentQuest(next.getCurrentQuest());
        WorldService.prepare(next);
        next.setCurrentChoices(story.choices(next));
        next.setCurrentStorySummary(response.getNarration());
        next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.GAME_MASTER,response.getNarration()));
        String result=changes(response)+(levelMessage.isBlank()?"":" "+levelMessage);
        if(!result.isBlank())next.getStoryLogs().add(new StoryLog(StoryLog.Speaker.SYSTEM,result.trim()));
        BondService.prepare(next);return new Transition(next,response,result.trim());
    }
    private String changes(StoryResponse response) {
        List<String> changes=new ArrayList<>();
        if(response.getHpChange()!=0)changes.add("HP "+signed(response.getHpChange()));
        if(response.getMpChange()!=0)changes.add("MP "+signed(response.getMpChange()));
        if(response.getExpGain()!=0)changes.add("EXP "+signed(response.getExpGain()));
        if(response.getGoldChange()!=0)changes.add("Gold "+signed(response.getGoldChange()));
        for(var item:response.getItemsGained())changes.add(item.getItem().getName()+" x"+item.getQuantity());
        return String.join(" / ",changes);
    }
    private String signed(int value) { return value>0?"+"+value:Integer.toString(value); }
}
