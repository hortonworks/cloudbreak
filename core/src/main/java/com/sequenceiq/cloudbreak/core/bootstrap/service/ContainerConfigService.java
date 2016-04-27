package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.bootstrap.config.GenericConfig;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class ContainerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerConfigService.class);

    @Value("${cb.docker.container.ambari.agent:}")
    private String ambariAgent;

    @Value("${cb.docker.container.ambari.server:}")
    private String ambariServer;

    @Value("${cb.docker.container.registrator:}")
    private String registratorDockerImageName;

    @Value("${cb.docker.container.docker.consul.watch.plugn:}")
    private String consulWatchPlugnDockerImageName;

    @Value("${cb.docker.container.ambari.db:}")
    private String postgresDockerImageName;

    @Value("${cb.docker.container.kerberos:}")
    private String kerberosDockerImageName;

    @Value("${cb.docker.container.logrotate:}")
    private String logrotateDockerImageName;

    @Value("${cb.docker.container.munchausen:}")
    private String munchausenImageName;

    @Value("${cb.docker.container.haveged:}")
    private String havegedImageName;

    @Value("${cb.docker.container.ldap:}")
    private String ldapImageName;

    @Value("${cb.docker.container.shipyard:}")
    private String shipyardImageName;

    @Value("${cb.docker.container.shipyard.db:}")
    private String rethinkDbImageName;

    @Inject
    private ComponentRepository componentRepository;

    public ContainerConfig get(Stack stack, DockerContainer dc) {
        try {
            Component component = componentRepository.findComponentByStackIdComponentTypeName(stack.getId(), ComponentType.CONTAINER, dc.name());
            if (component == null) {
                component = create(stack, dc);
                LOGGER.info("Container component definition created: {}", component);
            } else {
                LOGGER.info("Container component definition found in database: {}", component);
            }
            return component.getAttributes().get(ContainerConfig.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException(String.format("Failed to parse component ContainerConfig for stack: %d, container: %s"));
        }
    }

    private Component create(Stack stack, DockerContainer dc) {
        try {
            ContainerConfig config;
            switch (dc) {
                case AMBARI_SERVER:
                    config = new GenericConfig.Builder(ambariServer).build();
                    break;
                case AMBARI_AGENT:
                    config = new GenericConfig.Builder(ambariAgent).build();
                    break;
                case AMBARI_DB:
                    config = new GenericConfig.Builder(postgresDockerImageName).build();
                    break;
                case KERBEROS:
                    config = new GenericConfig.Builder(kerberosDockerImageName).build();
                    break;
                case REGISTRATOR:
                    config = new GenericConfig.Builder(registratorDockerImageName).build();
                    break;
                case MUNCHAUSEN:
                    config = new GenericConfig.Builder(munchausenImageName).build();
                    break;
                case CONSUL_WATCH:
                    config = new GenericConfig.Builder(consulWatchPlugnDockerImageName).build();
                    break;
                case LOGROTATE:
                    config = new GenericConfig.Builder(logrotateDockerImageName).build();
                    break;
                case HAVEGED:
                    config = new GenericConfig.Builder(havegedImageName).build();
                    break;
                case LDAP:
                    config = new GenericConfig.Builder(ldapImageName).build();
                    break;
                case SHIPYARD:
                    config = new GenericConfig.Builder(shipyardImageName).build();
                    break;
                case SHIPYARD_DB:
                    config = new GenericConfig.Builder(rethinkDbImageName).build();
                    break;
                default:
                    throw new CloudbreakServiceException(String.format("No configuration exist for %s", dc));
            }

            Component component = new Component(ComponentType.CONTAINER, dc.name(), new Json(config), stack);
            return componentRepository.save(component);
        } catch (IOException e) {
            throw new CloudbreakServiceException(String.format("Failed to parse component ContainerConfig for stack: %d, container: %s"));
        }
    }

}
