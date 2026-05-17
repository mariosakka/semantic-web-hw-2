package com.example.semanticweb.service;

import com.example.semanticweb.model.Book;
import com.example.semanticweb.model.BookChunk;
import com.example.semanticweb.model.UserProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private static final String NOT_FOUND = "I could not find this information in the local book database.";
    private static final Pattern WHO_WROTE = Pattern.compile("(?i)^who wrote\\s+(.+?)\\??$");
    private static final Pattern AUTHOR_THEME = Pattern.compile("(?i)^what book has the author\\s+(.+?)\\s+and the theme\\s+(.+?)\\??$");
    private static final Pattern BEST_USER = Pattern.compile("(?i)^which user is most likely to enjoy\\s+(.+?)\\??$");

    private final RdfService rdfService;
    private final RecommendationService recommendationService;
    private final EmbeddingService embeddingService;
    private final ElasticsearchService elasticsearchService;
    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String chatModel;

    public ChatbotService(
            RdfService rdfService,
            RecommendationService recommendationService,
            EmbeddingService embeddingService,
            ElasticsearchService elasticsearchService,
            RestClient openAiRestClient,
            ObjectMapper objectMapper,
            @Value("${openai.chat.model:gpt-4o-mini}") String chatModel
    ) {
        this.rdfService = rdfService;
        this.recommendationService = recommendationService;
        this.embeddingService = embeddingService;
        this.elasticsearchService = elasticsearchService;
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
    }

    public String answer(String question, Book currentBook) {
        if (question == null || question.isBlank()) {
            return "Please enter a question.";
        }

        String cleanedQuestion = question.trim();
        String exactAnswer = exactAnswer(cleanedQuestion, currentBook);
        if (exactAnswer != null) {
            return exactAnswer;
        }

        List<Float> embedding = embeddingService.createEmbedding(cleanedQuestion);
        if (embedding.isEmpty()) {
            return NOT_FOUND;
        }

        String context = elasticsearchService.searchByVector(embedding, 5).stream()
                .map(BookChunk::getText)
                .reduce("", (left, right) -> left + right + "\n");

        if (context.isBlank()) {
            return NOT_FOUND;
        }

        return Optional.ofNullable(askOpenAi(prompt(context, cleanedQuestion)))
                .filter(answer -> !answer.isBlank())
                .orElse(NOT_FOUND);
    }

    public List<String> startersForBooksPage() {
        return List.of(
                "What is a book that Alice is most likely to enjoy from this list?",
                "Which books are suitable for beginner readers?",
                "Show me books with the Science Fiction theme."
        );
    }

    public List<String> startersForBookPage(Book book) {
        String title = book == null ? "this book" : book.getTitle();
        return List.of(
                "Who is the author of this book?",
                "What themes does this book have?",
                "Would Alice enjoy this book?",
                "Who wrote " + title + "?"
        );
    }

    public List<String> startersForGeneralPage() {
        return List.of(
                "Who wrote The Amber Circuit?",
                "What book has the author Lorian Vey and the theme Science Fiction?",
                "Which books are suitable for beginner readers?"
        );
    }

    private String exactAnswer(String question, Book currentBook) {
        String lower = question.toLowerCase(Locale.ROOT);

        if (currentBook != null && lower.contains("author of this book")) {
            return local("the author is " + currentBook.getAuthor());
        }
        if (currentBook != null && lower.contains("themes does this book")) {
            return local("the themes are " + String.join(", ", currentBook.getThemes()));
        }
        if (currentBook != null && lower.contains("would alice enjoy this book")) {
            String result = recommendationService.wouldUserEnjoyBook("Alice", currentBook)
                    ? "Alice is likely to enjoy this book"
                    : "Alice is not likely to enjoy this book";
            return local(result);
        }
        if (lower.contains("alice") && lower.contains("most likely to enjoy") && lower.contains("from this list")) {
            return recommendationService.recommendBooksForUser("Alice").stream()
                    .findFirst()
                    .map(book -> local("Alice is most likely to enjoy " + book.getTitle()))
                    .orElse(NOT_FOUND);
        }
        if (lower.contains("beginner readers")) {
            return bookList("these books are suitable for beginner readers", rdfService.findBooksByReadingLevel("Beginner"));
        }
        if (lower.contains("science fiction") && lower.contains("theme")) {
            return bookList("these books have the Science Fiction theme", rdfService.findBooksByTheme("Science Fiction"));
        }

        return matchBookTitle(WHO_WROTE, question)
                .map(book -> local(book.getTitle() + " was written by " + book.getAuthor()))
                .or(() -> matchAuthorTheme(question))
                .or(() -> matchBestUser(question))
                .orElse(null);
    }

    private Optional<String> matchAuthorTheme(String question) {
        var matcher = AUTHOR_THEME.matcher(question);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(bookList("", rdfService.findBooksByAuthorAndTheme(clean(matcher.group(1)), clean(matcher.group(2)))))
                .filter(answer -> !NOT_FOUND.equals(answer));
    }

    private Optional<String> matchBestUser(String question) {
        return matchBookTitle(BEST_USER, question)
                .flatMap(book -> recommendationService.findBestUserForBook(book)
                        .map(user -> local(user.getName() + " is most likely to enjoy " + book.getTitle())));
    }

    private Optional<Book> matchBookTitle(Pattern pattern, String question) {
        var matcher = pattern.matcher(question);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return rdfService.getBookByTitle(clean(matcher.group(1)));
    }

    private String bookList(String prefix, List<Book> books) {
        if (books.isEmpty()) {
            return NOT_FOUND;
        }

        String titles = String.join(", ", books.stream().map(Book::getTitle).toList());
        if (prefix.isBlank()) {
            return titles + ".";
        }
        return local(prefix + ": " + titles);
    }

    private String askOpenAi(String prompt) {
        try {
            String response = openAiRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", chatModel,
                            "temperature", 0,
                            "messages", List.of(
                                    Map.of("role", "system", "content", "You are a book recommendation assistant."),
                                    Map.of("role", "user", "content", prompt)
                            )
                    ))
                    .retrieve()
                    .body(String.class);

            JsonNode choices = objectMapper.readTree(response).path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            return choices.get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String prompt(String context, String question) {
        return "You are a book recommendation assistant.\n\n" +
                "Answer using only the local database context below.\n" +
                "Do not use outside knowledge.\n" +
                "If the answer is not present in the context, say:\n" +
                "\"" + NOT_FOUND + "\"\n\n" +
                "Local database context:\n" +
                context + "\n" +
                "User question:\n" +
                question;
    }

    private String local(String answer) {
        return "According to the local book database, " + answer + ".";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("[?.!]+$", "").trim();
    }
}
