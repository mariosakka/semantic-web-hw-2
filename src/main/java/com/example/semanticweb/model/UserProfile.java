package com.example.semanticweb.model;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private String id;
    private String name;
    private List<String> preferredThemes = new ArrayList<>();
    private String preferredReadingLevel;

    public UserProfile() {
    }

    public UserProfile(String id, String name, List<String> preferredThemes, String preferredReadingLevel) {
        this.id = id;
        this.name = name;
        this.preferredThemes = preferredThemes == null ? new ArrayList<>() : new ArrayList<>(preferredThemes);
        this.preferredReadingLevel = preferredReadingLevel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPreferredThemes() {
        return preferredThemes;
    }

    public void setPreferredThemes(List<String> preferredThemes) {
        this.preferredThemes = preferredThemes == null ? new ArrayList<>() : new ArrayList<>(preferredThemes);
    }

    public String getPreferredReadingLevel() {
        return preferredReadingLevel;
    }

    public void setPreferredReadingLevel(String preferredReadingLevel) {
        this.preferredReadingLevel = preferredReadingLevel;
    }
}
