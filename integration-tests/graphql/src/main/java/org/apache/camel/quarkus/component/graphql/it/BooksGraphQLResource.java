package org.apache.camel.quarkus.component.graphql.it;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.component.graphql.it.model.Book;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@ApplicationScoped
public class BooksGraphQLResource {
    private static final List<Book> BOOKS = new ArrayList<>(List.of(
            new Book(1, "Harry Potter and the Philosophers Stone", "J.K Rowling"),
            new Book(2, "Moby Dick", "Herman Melville"),
            new Book(3, "Interview with the vampire", "Anne Rice")));

    @Query
    public List<Book> getBooks() {
        return BOOKS;
    }

    @Query
    public Book getBookById(int id) {
        return BOOKS.stream().filter(book -> book.getId() == id).findFirst().orElse(null);
    }

    @Mutation
    public Book addBook(Book bookInput) {
        Book book = new Book(bookInput.getId(), bookInput.getName(), bookInput.getAuthor());
        BOOKS.add(book);
        return book;
    }
}
