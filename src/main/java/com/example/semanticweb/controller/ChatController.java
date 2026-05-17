package com.example.semanticweb.controller;

import com.example.semanticweb.model.Book;
import com.example.semanticweb.service.BookService;
import com.example.semanticweb.service.ChatbotService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatController {

    private final ChatbotService chatbotService;
    private final BookService bookService;

    public ChatController(ChatbotService chatbotService, BookService bookService) {
        this.chatbotService = chatbotService;
        this.bookService = bookService;
    }

    @PostMapping("/chat")
    public String chat(
            @RequestParam String question,
            @RequestParam(required = false) String redirectPath,
            @RequestParam(required = false) String bookId,
            RedirectAttributes redirectAttributes
    ) {
        String safeRedirectPath = sanitizeRedirectPath(redirectPath);

        Book currentBook = bookId == null || bookId.isBlank()
                ? null
                : bookService.getBookById(bookId).orElse(null);

        String answer = chatbotService.answer(question, currentBook);
        redirectAttributes.addFlashAttribute("chatQuestion", question);
        redirectAttributes.addFlashAttribute("chatAnswer", answer);

        return "redirect:" + safeRedirectPath;
    }

    private String sanitizeRedirectPath(String redirectPath) {
        if (redirectPath == null || redirectPath.isBlank()) {
            return "/";
        }
        if (!redirectPath.startsWith("/") || redirectPath.contains("://")) {
            return "/";
        }
        return redirectPath;
    }
}
