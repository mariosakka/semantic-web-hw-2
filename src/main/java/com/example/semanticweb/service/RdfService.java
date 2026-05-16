package com.example.semanticweb.service;

import com.example.semanticweb.model.Book;
import com.example.semanticweb.model.UserProfile;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RdfService {

    public static final String NS = "http://example.com/book-recommendation#";

    private static final String DEMO_RESOURCE = "data/books.rdf";
    private static final Path LOCAL_DEMO_FILE = Path.of("src/main/resources/data/books.rdf");

    private final Path demoPath;

    public RdfService(
            @Value("${app.rdf.demo-path:/app/data/books.rdf}") String demoPath
    ) {
        this.demoPath = Path.of(demoPath);
    }

    public Model loadDemoModel() {
        return loadModel(demoPath, DEMO_RESOURCE);
    }

    public void saveDemoModel(Model model) {
        Path target = isWritable(demoPath.getParent()) ? demoPath : LOCAL_DEMO_FILE;

        try {
            Files.createDirectories(target.getParent());
            try (OutputStream outputStream = Files.newOutputStream(target)) {
                model.write(outputStream, "RDF/XML-ABBREV");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save RDF data", e);
        }
    }

    public List<Book> getAllBooks() {
        Model model = loadDemoModel();
        return model.listResourcesWithProperty(RDF.type, resource("Book"))
                .mapWith(this::toBook)
                .toList()
                .stream()
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<Book> getBookById(String bookId) {
        if (blank(bookId)) {
            return Optional.empty();
        }

        Model model = loadDemoModel();
        Resource book = model.getResource(NS + bookId.trim());

        if (model.contains(book, RDF.type, resource("Book"))) {
            return Optional.of(toBook(book));
        }

        return getAllBooks().stream()
                .filter(candidate -> bookId.equalsIgnoreCase(candidate.getId()))
                .findFirst();
    }

    public Optional<Book> getBookByTitle(String title) {
        if (blank(title)) {
            return Optional.empty();
        }

        return getAllBooks().stream()
                .filter(book -> title.trim().equalsIgnoreCase(book.getTitle()))
                .findFirst();
    }

    public void addBook(Book book) {
        Model model = loadDemoModel();
        String id = normalizeId(blank(book.getId()) ? book.getTitle() : book.getId());

        if (id.isBlank()) {
            throw new IllegalArgumentException("Book id or title is required");
        }

        Resource bookResource = model.createResource(NS + id);
        clearBookProperties(bookResource);

        bookResource.addProperty(RDF.type, resource("Book"));
        addLiteral(bookResource, "title", book.getTitle());

        if (!blank(book.getAuthor())) {
            bookResource.addProperty(property("hasAuthor"), namedResource(model, "Author", "author", book.getAuthor()));
        }

        new LinkedHashSet<>(book.getThemes()).stream()
                .filter(theme -> !blank(theme))
                .map(theme -> namedResource(model, "Theme", "theme", theme))
                .forEach(theme -> bookResource.addProperty(property("hasTheme"), theme));

        if (!blank(book.getReadingLevel())) {
            bookResource.addProperty(property("hasReadingLevel"), namedResource(model, "ReadingLevel", "level", book.getReadingLevel()));
        }

        saveDemoModel(model);
    }

    public void updateBookReadingLevel(String bookId, String readingLevel) {
        if (blank(bookId) || blank(readingLevel)) {
            return;
        }

        Model model = loadDemoModel();
        Resource book = model.getResource(NS + bookId.trim());
        if (!model.contains(book, RDF.type, resource("Book"))) {
            return;
        }

        book.removeAll(property("hasReadingLevel"));
        book.addProperty(property("hasReadingLevel"), namedResource(model, "ReadingLevel", "level", readingLevel));
        saveDemoModel(model);
    }

    public List<Book> findBooksByAuthorAndTheme(String author, String theme) {
        if (blank(author) || blank(theme)) {
            return List.of();
        }

        return getAllBooks().stream()
                .filter(book -> author.trim().equalsIgnoreCase(book.getAuthor()))
                .filter(book -> book.getThemes().stream().anyMatch(bookTheme -> theme.trim().equalsIgnoreCase(bookTheme)))
                .toList();
    }

    public List<Book> findBooksByTheme(String theme) {
        if (blank(theme)) {
            return List.of();
        }

        return getAllBooks().stream()
                .filter(book -> book.getThemes().stream().anyMatch(bookTheme -> theme.trim().equalsIgnoreCase(bookTheme)))
                .toList();
    }

    public List<Book> findBooksByReadingLevel(String readingLevel) {
        if (blank(readingLevel)) {
            return List.of();
        }

        return getAllBooks().stream()
                .filter(book -> readingLevel.trim().equalsIgnoreCase(book.getReadingLevel()))
                .toList();
    }

    public List<UserProfile> getAllUsers() {
        Model model = loadDemoModel();
        return model.listResourcesWithProperty(RDF.type, resource("User"))
                .mapWith(user -> toUser(user))
                .toList()
                .stream()
                .sorted(Comparator.comparing(UserProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<UserProfile> getUserByName(String userName) {
        if (blank(userName)) {
            return Optional.empty();
        }

        return getAllUsers().stream()
                .filter(user -> userName.trim().equalsIgnoreCase(user.getName()))
                .findFirst();
    }

    private Model loadModel(Path path, String classpathResource) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("ex", NS);

        try (InputStream inputStream = open(path, classpathResource)) {
            model.read(inputStream, null, "RDF/XML");
            return model;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load RDF model", e);
        }
    }

    private InputStream open(Path path, String classpathResource) throws IOException {
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }

        Path localPath = Path.of("src/main/resources").resolve(classpathResource);
        if (Files.exists(localPath)) {
            return Files.newInputStream(localPath);
        }

        ClassPathResource resource = new ClassPathResource(classpathResource);
        if (resource.exists()) {
            return resource.getInputStream();
        }

        throw new IllegalStateException("RDF file not found: " + path);
    }

    private Book toBook(Resource resource) {
        return new Book(
                id(resource),
                literal(resource, "title"),
                nameOf(object(resource, "hasAuthor")),
                objects(resource, "hasTheme").stream().map(this::nameOf).toList(),
                nameOf(object(resource, "hasReadingLevel"))
        );
    }

    private UserProfile toUser(Resource resource) {
        return new UserProfile(
                id(resource),
                literal(resource, "name"),
                objects(resource, "hasPreferredTheme").stream().map(this::nameOf).toList(),
                nameOf(object(resource, "hasPreferredReadingLevel"))
        );
    }

    private void clearBookProperties(Resource book) {
        List.of("title", "hasAuthor", "hasTheme", "hasReadingLevel")
                .forEach(name -> book.removeAll(property(name)));
    }

    private Resource namedResource(Model model, String className, String prefix, String value) {
        Resource resource = model.createResource(NS + prefix + "-" + normalizeId(value));
        resource.removeAll(property("name"));
        resource.addProperty(RDF.type, resource(className));
        addLiteral(resource, "name", value);
        return resource;
    }

    private List<Resource> objects(Resource subject, String propertyName) {
        return subject.listProperties(property(propertyName))
                .mapWith(Statement::getObject)
                .filterKeep(RDFNode::isResource)
                .mapWith(RDFNode::asResource)
                .toList();
    }

    private Resource object(Resource subject, String propertyName) {
        Statement statement = subject.getProperty(property(propertyName));
        if (statement == null || !statement.getObject().isResource()) {
            return null;
        }
        return statement.getObject().asResource();
    }

    private String literal(Resource subject, String propertyName) {
        Statement statement = subject.getProperty(property(propertyName));
        if (statement == null || !statement.getObject().isLiteral()) {
            return "";
        }
        return statement.getObject().asLiteral().getString();
    }

    private String nameOf(Resource resource) {
        if (resource == null) {
            return "";
        }

        String name = literal(resource, "name");
        return name.isBlank() ? resource.getLocalName() : name;
    }

    private void addLiteral(Resource resource, String propertyName, String value) {
        if (!blank(value)) {
            resource.addLiteral(property(propertyName), value.trim());
        }
    }

    private String id(Resource resource) {
        return resource.getLocalName() == null ? "" : resource.getLocalName();
    }

    private boolean isWritable(Path path) {
        return path != null && Files.exists(path) && Files.isWritable(path);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private Resource resource(String localName) {
        return ResourceFactory.createResource(NS + localName);
    }

    private Property property(String localName) {
        return ResourceFactory.createProperty(NS, localName);
    }
}
