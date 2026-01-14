package com.example.frontendproject;

import java.util.List;

public class VoteRequest {
    public long pollId;
    public String optionId;             // for Yes/No/MCQ
    public Integer rating;              // for rating polls (nullable)
    public List<String> ranking;        // for ranking polls (nullable)
    public String userId;               // put your logged-in user id
}
