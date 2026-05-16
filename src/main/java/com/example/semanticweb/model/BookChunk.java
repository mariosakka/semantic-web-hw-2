package com.example.semanticweb.model;

import java.util.ArrayList;
import java.util.List;

public class BookChunk {
    private Book book;
    private String text;
    private List<Float> embedding = new ArrayList<>();

    public BookChunk() {
    }

    public BookChunk(Book book, String text, List<Float> embedding) {
        this.book = book;
        this.text = text;
        this.embedding = embedding == null ? new ArrayList<>() : new ArrayList<>(embedding);
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding == null ? new ArrayList<>() : new ArrayList<>(embedding);
    }
}
