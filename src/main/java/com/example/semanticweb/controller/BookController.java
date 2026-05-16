package com.example.semanticweb.controller;

import com.example.semanticweb.model.Book;
import com.example.semanticweb.service.BookService;
import com.example.semanticweb.service.ChatbotService;
import com.example.semanticweb.service.RagIndexingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final RagIndexingService ragIndexingService;
    private final ChatbotService chatbotService;

    public BookController(BookService bookService, RagIndexingService ragIndexingService, ChatbotService chatbotService) {
        this.bookService = bookService;
        this.ragIndexingService = ragIndexingService;
        this.chatbotService = chatbotService;
    }

    @GetMapping
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        addBooksChat(model);
        return "books";
    }

    @GetMapping("/{bookId}")
    public String bookDetails(@PathVariable String bookId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Book> book = bookService.getBookById(bookId);
        if (book.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found.");
            return "redirect:/books";
        }

        model.addAttribute("book", book.get());
        addBookChat(model, book.get(), "/books/" + bookId);
        return "book-details";
    }

    @GetMapping("/add")
    public String addBookPage(Model model) {
        model.addAttribute("readingLevels", List.of("Beginner", "Intermediate", "Advanced"));
        addGeneralChat(model, "/books/add");
        return "add-book";
    }

    @PostMapping("/add")
    public String addBook(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String themes,
            @RequestParam String readingLevel,
            RedirectAttributes redirectAttributes
    ) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Title and author are required.");
            return "redirect:/books/add";
        }

        String id = UUID.randomUUID().toString();
        List<String> parsedThemes = Arrays.stream(themes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        Book book = new Book(id, title.trim(), author.trim(), parsedThemes, readingLevel.trim());
        bookService.addBook(book);
        ragIndexingService.reindexBook(id);

        redirectAttributes.addFlashAttribute("message", "Book added successfully.");
        return "redirect:/books/" + id;
    }

    @GetMapping("/{bookId}/edit")
    public String editBookPage(@PathVariable String bookId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Book> book = bookService.getBookById(bookId);
        if (book.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found.");
            return "redirect:/books";
        }

        model.addAttribute("book", book.get());
        model.addAttribute("readingLevels", List.of("Beginner", "Intermediate", "Advanced"));
        addBookChat(model, book.get(), "/books/" + bookId + "/edit");
        return "edit-book";
    }

    @PostMapping("/{bookId}/edit")
    public String editBook(
            @PathVariable String bookId,
            @RequestParam String readingLevel,
            RedirectAttributes redirectAttributes
    ) {
        bookService.updateBookReadingLevel(bookId, readingLevel);
        ragIndexingService.reindexBook(bookId);

        redirectAttributes.addFlashAttribute("message", "Reading level updated successfully.");
        return "redirect:/books/" + bookId;
    }

    private void addBooksChat(Model model) {
        model.addAttribute("chatStarters", chatbotService.startersForBooksPage());
        model.addAttribute("chatRedirectPath", "/books");
        model.addAttribute("chatBookId", "");
    }

    private void addGeneralChat(Model model, String redirectPath) {
        model.addAttribute("chatStarters", chatbotService.startersForGeneralPage());
        model.addAttribute("chatRedirectPath", redirectPath);
        model.addAttribute("chatBookId", "");
    }

    private void addBookChat(Model model, Book book, String redirectPath) {
        model.addAttribute("chatStarters", chatbotService.startersForBookPage(book));
        model.addAttribute("chatRedirectPath", redirectPath);
        model.addAttribute("chatBookId", book.getId());
    }
}
