package com.example.frontendproject;

public class ChatMessage {
    public static final int ROLE_USER = 0;
    public static final int ROLE_BOT  = 1;

    private final int role;
    private final String text;

    public ChatMessage(int role, String text) {
        this.role = role;
        this.text = text;
    }

    public int getRole() { return role; }
    public String getText() { return text; }
}
