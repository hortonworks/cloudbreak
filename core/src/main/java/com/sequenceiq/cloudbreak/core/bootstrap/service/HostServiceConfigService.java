package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.bootstrap.config.HostServiceConfigBuilder;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.orchestrator.container.HostServiceType;
import com.sequenceiq.cloudbreak.orchestrator.model.HostServiceConfig;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class HostServiceConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostServiceConfigService.class);

    @Value("${cb.host.service.ambari.agent.version:}")
    private String ambariAgentVersion;

    @Value("${cb.host.service.ambari.repo.url:}")
    private String ambariRepoUrl;

    @Value("${cb.host.service.ambari.server.version:}")
    private String ambariServerVersion;

    @Inject
    private ComponentRepository componentRepository;

    public HostServiceConfig get(Stack stack, HostServiceType ht) {
        try {
            Component component = componentRepository.findComponentByStackIdComponentTypeName(stack.getId(), ComponentType.SERVICE, ht.name());
            if (component == null) {
                component = create(stack, ht);
                LOGGER.info("Host service component definition created: {}", component);
            } else {
                LOGGER.info("Host service component definition found in database: {}", component);
            }
            return component.getAttributes().get(HostServiceConfig.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException(String.format("Failed to parse component HostServiceConfig for stack: %d, service: %s"));
        }
    }

    private Component create(Stack stack, HostServiceType ht) {
        try {
            HostServiceConfig config;
            switch (ht) {
                case AMBARI_SERVER:
                    config = new HostServiceConfigBuilder().builder()
                            .withName(HostServiceType.AMBARI_SERVER.getName())
                            .withRepoUrl(ambariRepoUrl)
                            .withVersion(ambariServerVersion)
                            .build();
                    break;
                case AMBARI_AGENT:
                    config = new HostServiceConfigBuilder().builder()
                            .withName(HostServiceType.AMBARI_AGENT.getName())
                            .withRepoUrl(ambariRepoUrl)
                            .withVersion(ambariAgentVersion)
                            .build();
                    break;
                default:
                    throw new CloudbreakServiceException(String.format("No configuration exist for %s", ht));
            }

            Component component = new Component(ComponentType.SERVICE, ht.name(), new Json(config), stack);
            return componentRepository.save(component);
        } catch (IOException e) {
            throw new CloudbreakServiceException(String.format("Failed to parse component HostServiceConfig for stack: %d, service: %s"));
        }
    }

}
