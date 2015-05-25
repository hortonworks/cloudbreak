package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CONTAINER_ORCHESTRATOR;

import java.util.Map;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;

@Component
public class ContainerOrchestratorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerOrchestratorResolver.class);

    @Value("${cb.container.orchestrator:" + CB_CONTAINER_ORCHESTRATOR + "}")
    private String containerOrchestratorName;

    @Resource
    private Map<String, ContainerOrchestrator> containerOrchestrators;

    public ContainerOrchestrator get() throws CloudbreakException {
        ContainerOrchestrator co = containerOrchestrators.get(containerOrchestratorName);
        if (co == null) {
            LOGGER.error("ContainerOrchestrator not found: {}, supported ContainerOrchestrators: {}", containerOrchestratorName, containerOrchestrators);
            throw new CloudbreakException("ContainerOrchestrator not found: " + containerOrchestratorName);
        }
        return containerOrchestrators.get(containerOrchestratorName);
    }
}
