package com.example.semanticweb.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String id;
    private String title;
    private String author;
    private List<String> themes = new ArrayList<>();
    private String readingLevel;

    public Book() {
    }

    public Book(String id, String title, String author, List<String> themes, String readingLevel) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.themes = themes == null ? new ArrayList<>() : new ArrayList<>(themes);
        this.readingLevel = readingLevel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes == null ? new ArrayList<>() : new ArrayList<>(themes);
    }

    public String getReadingLevel() {
        return readingLevel;
    }

    public void setReadingLevel(String readingLevel) {
        this.readingLevel = readingLevel;
    }
}
