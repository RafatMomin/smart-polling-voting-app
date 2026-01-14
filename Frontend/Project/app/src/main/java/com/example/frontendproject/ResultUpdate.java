package com.example.frontendproject;

import java.util.List;

public class ResultUpdate {
    public long pollId;
    public String status;        // "OPEN" or "CLOSED"
    public int totalVotes;
    public String lastUpdated;
    public List<Bucket> distribution;  // 👈 renamed from tallies

    public static class Bucket {
        public String optionId;
        public int count;
        public double percent;
    }
}
