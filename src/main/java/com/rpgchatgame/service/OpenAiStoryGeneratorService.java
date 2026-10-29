package com.rpgchatgame.service;

import com.rpgchatgame.model.GameState;
import com.rpgchatgame.model.StoryResponse;

public class OpenAiStoryGeneratorService implements StoryGeneratorService {
    @Override
    public StoryResponse generateNextScene(GameState gameState, String playerAction) {
        // TODO: Replace LocalStoryGeneratorService with an OpenAI API-backed implementation here.
        throw new UnsupportedOperationException("OpenAI story generation is not implemented in the MVP.");
    }
}
