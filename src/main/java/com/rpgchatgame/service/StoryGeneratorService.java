package com.rpgchatgame.service;

import com.rpgchatgame.model.GameState;
import com.rpgchatgame.model.StoryResponse;

public interface StoryGeneratorService {
    StoryResponse generateNextScene(GameState gameState, String playerAction);
}
