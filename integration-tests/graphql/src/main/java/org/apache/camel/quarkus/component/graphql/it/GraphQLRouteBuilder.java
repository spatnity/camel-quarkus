package org.apache.camel.quarkus.component.graphql.it;

import org.apache.camel.builder.RouteBuilder;

public class GraphQLRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:addBookGraphQL")
                .toD("graphql://http://localhost:${header.port}/graphql?queryFile=graphql/addBookMutation.graphql&operationName=AddBook");

        from("direct:getBookGraphQL")
                .toD("graphql://http://localhost:${header.port}/graphql?queryFile=graphql/bookQuery.graphql&operationName=BookById");

        from("direct:getQuery")
                .toD("graphql://http://localhost:${header.port}/graphql?query={books{id name}}");
    }
}
