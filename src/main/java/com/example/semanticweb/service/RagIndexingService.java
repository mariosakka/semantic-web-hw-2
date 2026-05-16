package com.example.semanticweb.service;

import com.example.semanticweb.model.Book;
import com.example.semanticweb.model.BookChunk;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagIndexingService {

    private final RdfService rdfService;
    private final EmbeddingService embeddingService;
    private final ElasticsearchService elasticsearchService;

    public RagIndexingService(
            RdfService rdfService,
            EmbeddingService embeddingService,
            ElasticsearchService elasticsearchService
    ) {
        this.rdfService = rdfService;
        this.embeddingService = embeddingService;
        this.elasticsearchService = elasticsearchService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        elasticsearchService.ensureIndex();
        if (embeddingService.isEnabled()) {
            indexAllBooks();
        }
    }

    public void indexAllBooks() {
        List<Book> books = rdfService.getAllBooks();
        for (Book book : books) {
            indexBook(book);
        }
    }

    public void reindexBook(String bookId) {
        rdfService.getBookById(bookId).ifPresent(this::indexBook);
    }

    private void indexBook(Book book) {
        BookChunk chunk = toChunk(book);
        List<Float> embedding = embeddingService.createEmbedding(chunk.getText());
        if (embedding.isEmpty()) {
            return;
        }
        chunk.setEmbedding(embedding);
        elasticsearchService.indexChunk(chunk);
    }

    private BookChunk toChunk(Book book) {
        String text = "Book: " + book.getTitle() + ". " +
                "Author: " + book.getAuthor() + ". " +
                "Themes: " + String.join(", ", book.getThemes()) + ". " +
                "Reading level: " + book.getReadingLevel() + ".";

        return new BookChunk(book, text, List.of());
    }
}
