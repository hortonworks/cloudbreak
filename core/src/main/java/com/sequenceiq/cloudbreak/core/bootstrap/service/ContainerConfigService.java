package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.CUSTOM_QUEUE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.config.ContainerConfigBuilder.Builder;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Service
public class ContainerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerConfigService.class);

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private SecretService secretService;

    public ContainerConfig get(Stack stack, DockerContainer dc) {
        try {
            Component component = componentConfigProvider.getComponent(stack.getId(), ComponentType.CONTAINER, dc.name());
            if (component == null) {
                component = create(stack, dc);
                LOGGER.info("Container component definition created: {}", component);
            } else {
                LOGGER.info("Container component definition found in database: {}", component);
            }
            return component.getAttributes().get(ContainerConfig.class);
        } catch (CloudbreakException | IOException ignored) {
            throw new CloudbreakServiceException(String.format("Failed to parse component ContainerConfig for stack: %d, container: %s", stack.getId(),
                    dc.getName()));
        }
    }

    private Component create(Stack stack, DockerContainer dc) throws CloudbreakException {
        try {
            ContainerConfig config;
            ContainerOrchestrator orchestrator = containerOrchestratorResolver.get(stack.getOrchestrator().getType());
            Map<String, String> customContainerConfig = getCustomContainerConfig(stack);
            Optional<String> customContainerName = Optional.ofNullable(customContainerConfig.get(dc.name()));
            Optional<String> customQueue = getCustomQueue(stack);
            switch (dc) {
                case AMBARI_SERVER:
                    config = new Builder(orchestrator.ambariServerContainer(customContainerName), customQueue).build();
                    break;
                case AMBARI_AGENT:
                    config = new Builder(orchestrator.ambariClientContainer(customContainerName), customQueue).build();
                    break;
                case AMBARI_DB:
                    config = new Builder(orchestrator.ambariDbContainer(customContainerName), customQueue).build();
                    break;
                default:
                    throw new CloudbreakServiceException(String.format("No configuration exist for %s", dc));
            }

            Component component = new Component(ComponentType.CONTAINER, dc.name(), new Json(config), stack);
            return componentConfigProvider.store(component);
        } catch (IOException ignored) {
            throw new CloudbreakServiceException(String.format("Failed to parse component ContainerConfig for stack: %d, container: %s",
                    stack.getId(), dc.getName()));
        }
    }

    private Map<String, String> getCustomContainerConfig(Stack stack) {
        Json customContainerDefinition = stack.getCluster().getCustomContainerDefinition();
        if (customContainerDefinition != null && StringUtils.isNoneEmpty(customContainerDefinition.getValue())) {
            try {
                return customContainerDefinition.get(Map.class);
            } catch (IOException e) {
                LOGGER.error("Failed to add customContainerDefinition to response", e);
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    private Optional<String> getCustomQueue(Stack stack) {
        Json fromVault = new Json(secretService.get(stack.getCluster().getAttributes()));
        return Optional.ofNullable((String) fromVault.getMap().get(CUSTOM_QUEUE.name()));
    }

}
