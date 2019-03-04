package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Component
public class ContainerOrchestratorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerOrchestratorResolver.class);

    @Resource
    private Map<String, ContainerOrchestrator> containerOrchestrators;

    public ContainerOrchestrator get(String name) throws CloudbreakException {
        ContainerOrchestrator co = containerOrchestrators.get(name);
        if (co == null) {
            LOGGER.error("ContainerOrchestrator not found: {}, supported ContainerOrchestrators: {}", name, containerOrchestrators);
            throw new CloudbreakException("ContainerOrchestrator not found: " + name);
        }
        return co;
    }

}
