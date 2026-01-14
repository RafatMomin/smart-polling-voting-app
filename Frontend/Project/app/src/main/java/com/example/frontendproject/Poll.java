package com.example.frontendproject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Poll implements Serializable {
    public int id;
    public String title;
    public String type;
    public boolean now;

    public List<String> choices = new ArrayList<>();

    public String visibility = "PUBLIC";
    public boolean hasAccessCode = false;
    public boolean isMember = true;

    public String context = "";
    public boolean active = true;

    public boolean isPrivate() {
        return "PRIVATE".equalsIgnoreCase(visibility);
    }
}
