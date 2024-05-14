/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.graphql.it;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.graphql.it.model.Book;
import org.apache.camel.util.json.JsonObject;

@Path("/graphql")
public class GraphQLResource {

    private static final List<Book> BOOKS = new ArrayList<>(List.of(
            new Book("book-1", "Harry Potter and the Philosophers Stone", "author-1"),
            new Book("book-2", "Moby Dick", "author-2"),
            new Book("book-3", "Interview with the vampire", "author-3")));

    @Inject
    ProducerTemplate producerTemplate;

    public void setupRouter(@Observes Router router) {
        SchemaParser schemaParser = new SchemaParser();
        final TypeDefinitionRegistry typeDefinitionRegistry;
        try (Reader r = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("graphql/schema.graphql"),
                StandardCharsets.UTF_8)) {
            typeDefinitionRegistry = schemaParser.parse(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataFetcher<CompletionStage<Book>> dataFetcher = environment -> {
            CompletableFuture<Book> completableFuture = new CompletableFuture<>();
            Book book = getBookById(environment);
            completableFuture.complete(book);
            return completableFuture;
        };

        DataFetcher<CompletionStage<Book>> addBookDataFetcher = environment -> {
            CompletableFuture<Book> completableFuture = new CompletableFuture<>();
            completableFuture.complete(addBook(environment));
            return completableFuture;
        };

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("bookById", dataFetcher))
                .type("Mutation", builder -> builder.dataFetcher("addBook", addBookDataFetcher))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        router.post().handler(BodyHandler.create());
        router.route("/graphql/server").handler(GraphQLHandler.create(graphQL));
    }

    @Path("/queryFile")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response multipleQueries(@QueryParam("testPort") int port, @QueryParam("bookId") String bookId) {
        JsonObject variables = new JsonObject();
        variables.put("id", bookId);

        final Map<String, Object> headers = Map.of("port", port);

        final String result = producerTemplate.requestBodyAndHeaders(
                "direct:getBookGraphQL",
                variables, headers,
                String.class);

        return Response
                .ok()
                .entity(result)
                .build();
    }

    @Path("/mutation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response mutation(
            @QueryParam("testPort") int port,
            @QueryParam("authorId") String authorId,
            @QueryParam("name") String name) {

        JsonObject bookInput = new JsonObject();
        bookInput.put("name", name);
        bookInput.put("authorId", authorId);
        JsonObject variables = new JsonObject();
        variables.put("bookInput", bookInput);

        final Map<String, Object> headers = Map.of("port", port);

        final String result = producerTemplate.requestBodyAndHeaders(
                "direct:addBookGraphQL",
                variables, headers,
                String.class);

        return Response
                .ok()
                .entity(result)
                .build();
    }

    // @Path("/query")
    // @GET
    // @Produces(MediaType.APPLICATION_JSON)
    // public Response booksQueryWithStaticQuery(@QueryParam("testPort") int port){

    //     final Map<String, Object> headers = Map.of("port", port);

    //     String result = producerTemplate.requestBodyAndHeaders("direct:getQuery", null, headers String.class);

    //     return Response.ok().entity(result).build();
    // }

    private Book getBookById(DataFetchingEnvironment environment) {
        String bookId = environment.getArgument("id");
        return BOOKS.stream().filter(book -> book.getId().equals(bookId)).findFirst().orElse(null);
    }

    private Book addBook(DataFetchingEnvironment environment) {
        Map<String, String> bookInput = environment.getArgument("bookInput");
        String id = "book-" + (BOOKS.size() + 1);
        String name = bookInput.get("name");
        String authorId = bookInput.get("authorId");
        Book book = new Book(id, name, authorId);
        BOOKS.add(book);
        return book;
    }
}
