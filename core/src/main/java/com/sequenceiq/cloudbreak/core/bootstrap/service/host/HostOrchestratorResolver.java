package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Component
public class HostOrchestratorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostOrchestratorResolver.class);

    @Resource
    private Map<String, HostOrchestrator> hostOrchestrators;

    public HostOrchestrator get(String name) throws CloudbreakException {
        HostOrchestrator co = hostOrchestrators.get(name);
        if (co == null) {
            LOGGER.error("HostOrchestrator not found: {}, supported HostOrchestrator: {}", name, hostOrchestrators);
            throw new CloudbreakException("HostOrchestrator not found: " + name);
        }
        return co;
    }
}
