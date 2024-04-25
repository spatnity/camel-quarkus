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
package org.apache.camel.quarkus.component.langchain.chat.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.chat.LangChainChat;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

@Path("/langchain-chat")
@ApplicationScoped
public class LangchainChatResource {

    private static final Logger LOG = Logger.getLogger(LangchainChatResource.class);

    // private static final String COMPONENT_LANGCHAIN_CHAT = "langchain-chat";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:result")
    private MockEndpoint mock;

    @Path("/simple-message")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendSimpleMessage() throws Exception {
        //  mockEndpoint.expectedMessageCount(1);

        String response = producerTemplate.requestBody("direct:send-simple-message", "Hello my name is Darth Vader!",
                String.class);
        // mockEndpoint.assertIsSatisfied();
        return Response.noContent().build();
    }

    @Path("/promt-message")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendMessagewithPromt() throws Exception {

        var promptTemplate = "Create a recipe for a {{dishType}} with the following ingredients: {{ingredients}}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("dishType", "oven dish");
        variables.put("ingredients", "potato, tomato, feta, olive oil");

        String response = producerTemplate.requestBodyAndHeader("direct:send-message-prompt", variables,
                LangChainChat.Headers.PROMPT_TEMPLATE, promptTemplate, String.class);
        /*  mockEndpoint.assertIsSatisfied();

        assertTrue(response.contains("potato"));
        assertTrue(response.contains("tomato"));
        assertTrue(response.contains("feta"));
        assertTrue(response.contains("olive oil"));*/
        return Response.noContent().build();

    }

    @Path("/multiple-messages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendMultipleMessage() throws Exception {
        //  mockEndpoint.expectedMessageCount(1);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are asked to provide recommendations for a restaurant based on user reviews."));
        messages.add(new UserMessage("Hello, my name is Karen."));
        messages.add(new AiMessage("Hello Karen, how can I help you?"));
        messages.add(new UserMessage("I'd like you to recommend a restaurant for me."));
        messages.add(new AiMessage("Sure, what type of cuisine are you interested in?"));
        messages.add(new UserMessage("I'd like Moroccan food."));
        messages.add(new AiMessage("Sure, do you have a preference for the location?"));
        messages.add(new UserMessage("Paris, Rue Montorgueil."));

        String response = producerTemplate.requestBody("direct:send-multiple", messages, String.class);
        /*   mockEndpoint.assertIsSatisfied();

        assertNotNull(response);*/
        return Response.noContent().build();

    }

}
