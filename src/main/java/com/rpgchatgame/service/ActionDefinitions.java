package com.rpgchatgame.service;

import java.util.*;
import com.rpgchatgame.util.JsonUtil;

public final class ActionDefinitions {
    private static final Map<String,String> LABELS=load();
    private ActionDefinitions() {}
    @SuppressWarnings("unchecked") private static Map<String,String> load() {
        try(var input=ActionDefinitions.class.getResourceAsStream("/definitions/actions.json")) {
            if(input==null)throw new IllegalStateException("Action definitions missing");
            Map<String,String> labels=JsonUtil.fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),Map.class);
            if(labels.isEmpty()||labels.entrySet().stream().anyMatch(e->e.getKey().isBlank()||e.getValue().isBlank()))throw new IllegalStateException("Invalid action labels");
            return Map.copyOf(labels);
        }catch(java.io.IOException error){throw new ExceptionInInitializerError(error);}
    }
    public static String label(String action,String fallback){return LABELS.getOrDefault(action,fallback);}
}
