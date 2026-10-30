package com.rpgchatgame.model;

/** Domain mode, independent of which JavaFX menu/overlay is displayed. */
public enum GameplayMode {
    EXPLORATION, DIALOGUE, BATTLE, EPILOGUE;
    public static GameplayMode of(GameState state){
        if(state==null)throw new IllegalArgumentException("Missing state");
        var world=state.getCampaign();
        if(world.getEnemy()!=null)return BATTLE;
        if(world.epilogueClosed())return EPILOGUE;
        if(!world.getDialogueNode().isBlank())return DIALOGUE;
        return EXPLORATION;
    }
}
