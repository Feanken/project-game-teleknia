package com.rpgchatgame.model;

import java.time.LocalDateTime;

public class StoryLog {
    public enum Speaker {
        GAME_MASTER, PLAYER, SYSTEM
    }

    private long id;
    private Speaker speaker;
    private String message;
    private LocalDateTime createdAt;

    public StoryLog() {
    }

    public StoryLog(Speaker speaker, String message) {
        this.speaker = speaker;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public StoryLog(long id, Speaker speaker, String message, LocalDateTime createdAt) {
        this.id = id;
        this.speaker = speaker;
        this.message = message;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
