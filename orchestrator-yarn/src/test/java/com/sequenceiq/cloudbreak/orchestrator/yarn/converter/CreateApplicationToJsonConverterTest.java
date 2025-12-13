package com.sequenceiq.cloudbreak.orchestrator.yarn.converter;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.yarn.converter.request.CreateApplicationRequestToJsonConverter;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Artifact;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Dependency;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Resource;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;

class CreateApplicationToJsonConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateApplicationToJsonConverterTest.class);

    private static final String NAME = "testApp";

    private static final int LIFETIME = 12;

    private static final List<YarnComponent> COMPONENTS = new ArrayList<>();

    private static final String COMPONENT_NAME = "testComponent";

    private static final int NUM_OF_CONTAINTERS = 1;

    private static final String LAUNCH_COMMAND = "/bin/foo";

    private static final List<Dependency> DEPENDENCIES = new ArrayList<>();

    private static final String DEPENDENCY_ITEM = "dependencyItem";

    private static final String ARTIFACT_ID = "image/foo:test";

    private static final String ARTIFACT_TYPE = "DOCKER";

    private static final int RESOURCE_CPUS = 1;

    private static final int RESOURCE_MEMORY = 1024;

    @Test
    void testConvert() throws Exception {

        //CreateApplicationRequest object
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName(NAME);
        createApplicationRequest.setLifetime(LIFETIME);
        createApplicationRequest.setComponents(COMPONENTS);

        // Create a component
        YarnComponent component = new YarnComponent();

        // Create the Dependency
        Dependency dependency = new Dependency();
        dependency.setItem(DEPENDENCY_ITEM);
        DEPENDENCIES.add(dependency);

        // Create the Artifact
        Artifact artifact = new Artifact();
        artifact.setId(ARTIFACT_ID);
        artifact.setType(ARTIFACT_TYPE);

        // Create the Resource
        Resource resource = new Resource();
        resource.setCpus(RESOURCE_CPUS);
        resource.setMemory(RESOURCE_MEMORY);

        // Populate the component
        component.setName(COMPONENT_NAME);
        component.setDependencies(DEPENDENCIES);
        component.setNumberOfContainers(NUM_OF_CONTAINTERS);
        component.setArtifact(artifact);
        component.setLaunchCommand(LAUNCH_COMMAND);
        component.setResource(resource);

        // Add the component to the Create Application Request
        COMPONENTS.add(component);

        CreateApplicationRequestToJsonConverter createApplicationRequestToJsonConverter = new CreateApplicationRequestToJsonConverter();
        String jsonResult = createApplicationRequestToJsonConverter.convert(createApplicationRequest);

        LOGGER.info(jsonResult);

    }
}