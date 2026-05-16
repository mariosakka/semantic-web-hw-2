package com.example.semanticweb.service;

import com.example.semanticweb.model.Book;
import com.example.semanticweb.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RecommendationService {

    private final RdfService rdfService;

    public RecommendationService(RdfService rdfService) {
        this.rdfService = rdfService;
    }

    public boolean wouldUserEnjoyBook(String userName, Book book) {
        Optional<UserProfile> user = rdfService.getUserByName(userName);
        return user.filter(userProfile -> scoreBookForUser(book, userProfile) > 0).isPresent();
    }

    public Optional<UserProfile> findBestUserForBook(Book book) {
        return rdfService.getAllUsers().stream()
                .max(Comparator.comparingInt(user -> scoreBookForUser(book, user)));
    }

    public List<Book> recommendBooksForUser(String userName) {
        Optional<UserProfile> user = rdfService.getUserByName(userName);
        if (user.isEmpty()) {
            return List.of();
        }

        UserProfile userProfile = user.get();
        List<Book> books = new ArrayList<>(rdfService.getAllBooks());
        books.sort((left, right) -> Integer.compare(scoreBookForUser(right, userProfile), scoreBookForUser(left, userProfile)));

        return books.stream().filter(book -> scoreBookForUser(book, userProfile) > 0).toList();
    }

    private int scoreBookForUser(Book book, UserProfile userProfile) {
        int score = 0;

        for (String preferredTheme : userProfile.getPreferredThemes()) {
            boolean match = book.getThemes().stream().anyMatch(theme -> theme.equalsIgnoreCase(preferredTheme));
            if (match) {
                score += 2;
            }
        }

        if (book.getReadingLevel() != null
                && userProfile.getPreferredReadingLevel() != null
                && book.getReadingLevel().equalsIgnoreCase(userProfile.getPreferredReadingLevel())) {
            score += 1;
        }

        return score;
    }
}
