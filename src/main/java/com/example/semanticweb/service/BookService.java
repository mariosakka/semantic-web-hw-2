package com.example.semanticweb.service;

import com.example.semanticweb.model.Book;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final RdfService rdfService;

    public BookService(RdfService rdfService) {
        this.rdfService = rdfService;
    }

    public List<Book> getAllBooks() {
        return rdfService.getAllBooks();
    }

    public Optional<Book> getBookById(String bookId) {
        return rdfService.getBookById(bookId);
    }

    public void addBook(Book book) {
        rdfService.addBook(book);
    }

    public void updateBookReadingLevel(String bookId, String readingLevel) {
        rdfService.updateBookReadingLevel(bookId, readingLevel);
    }
}
