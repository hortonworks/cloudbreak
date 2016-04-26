package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;

@Component
public class ContainerOrchestratorTypeResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerOrchestratorTypeResolver.class);

    @Resource
    private Map<String, HostOrchestrator> hostOrchestrators;

    @Resource
    private Map<String, ContainerOrchestrator> containerOrchestrators;

    public ContainerOrchestratorType resolveType(String name) throws CloudbreakException {
        if (hostOrchestrators.keySet().contains(name)) {
            return ContainerOrchestratorType.HOST;
        } else if (containerOrchestrators.keySet().contains(name)) {
            return ContainerOrchestratorType.CONTAINER;
        } else {
            LOGGER.error("Orchestrator not found: {}", name);
            throw new CloudbreakException("Orchestrator not found: " + name);
        }
    }


}
