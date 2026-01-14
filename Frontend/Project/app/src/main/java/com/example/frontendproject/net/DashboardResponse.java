package com.example.frontendproject.net;

import java.util.List;

public class DashboardResponse {
    public int pollsJoined;
    public int votesCast;
    public PollTypesStats pollTypes;
    public List<RecentActivityItem> recentActivity;
}
