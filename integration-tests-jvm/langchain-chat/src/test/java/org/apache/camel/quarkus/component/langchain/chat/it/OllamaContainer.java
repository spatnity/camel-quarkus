package org.apache.camel.quarkus.component.langchain.chat.it;

import java.io.IOException;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class OllamaContainer extends GenericContainer<OllamaContainer> {

    private final DockerImageName dockerImageName;

    private final Integer port;
    private final String model;
    private final String imageName;

    public OllamaContainer(DockerImageName image, Integer port, String model, String imageName) {
        super(image);

        this.dockerImageName = image;
        this.port = port;
        this.model = model;
        this.imageName = imageName;

        withExposedPorts(port)
                .withImagePullPolicy(dockerImageName -> !dockerImageName.getVersionPart().endsWith(model))
                .setWaitStrategy(Wait.forListeningPort());
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (!this.dockerImageName.equals(DockerImageName.parse(imageName))) {
            try {
                execInContainer("ollama", "pull", model);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error pulling model", e);
            }
        }
    }
}
