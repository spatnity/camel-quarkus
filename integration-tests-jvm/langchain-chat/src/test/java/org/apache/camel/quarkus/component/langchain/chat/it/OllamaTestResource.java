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

import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

public class OllamaTestResource<T extends OllamaContainer> implements QuarkusTestResourceLifecycleManager {

    private static final int OLLAMA_PORT = 11434;
    private static final String OLLAMA_IMAGE = "docker.io/ollama/ollama:0.1.27";
    private static final String OLLAMA_MODEL = "orca-mini";

    public static final String LOCAL_OLLAMA_IMAGE = String.format("tc-%s-%s", OLLAMA_IMAGE, OLLAMA_MODEL);

    private OllamaContainer container;

    private DockerImageName dockerImageName;

    @Override
    public Map<String, String> start() {
        try {
            dockerImageName = resolveImageName();

            container = new OllamaContainer(dockerImageName, OLLAMA_PORT, OLLAMA_MODEL, LOCAL_OLLAMA_IMAGE);

            container.start();

            return CollectionHelper.mapOf(
                    "ollama.base.url", getBaseUrl()

            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected DockerImageName resolveImageName() {
        DockerImageName dockerImageName = DockerImageName.parse(OLLAMA_IMAGE);
        DockerClient dockerClient = DockerClientFactory.instance().client();
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(LOCAL_OLLAMA_IMAGE).exec();
        if (images.isEmpty()) {
            return dockerImageName;
        }
        return DockerImageName.parse(LOCAL_OLLAMA_IMAGE);
    }

    public String getBaseUrl() {
        return "http://" + container.getHost() + ":" + container.getMappedPort(OLLAMA_PORT);
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception ex) {
            //LOG.error("An issue occured while stopping the Container", ex);
        }
    }

}
