package onetoone.Dashboard.dto;

import java.time.LocalDateTime;

public class RecentActivity {
    private String pollName;
    private String action;
    private LocalDateTime time;

    public RecentActivity(String pollName, String action, LocalDateTime time) {
        this.pollName = pollName;
        this.action = action;
        this.time = time;
    }

    public String getPollName() {
        return pollName;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
