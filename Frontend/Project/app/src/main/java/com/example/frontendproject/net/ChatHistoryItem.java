package com.example.frontendproject.net;

import java.util.List;

public class ChatHistoryItem {
    public int id;
    public User user;
    public String userMessage;
    public String botResponse;
    public String timestamp;
    public List<RefPoll> referencedPolls;
    public Long responseTimeMs;

    public static class User {
        public String emailId;
        public String name;
    }

    public static class RefPoll {
        public int id;
        public String title;
        public String type;
        public boolean active;
    }
}
