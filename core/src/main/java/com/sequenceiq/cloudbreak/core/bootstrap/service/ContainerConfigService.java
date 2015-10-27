package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_DB;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_WARMUP;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_BAYWATCH_CLIENT;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_BAYWATCH_SERVER;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_KERBEROS;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_LOGROTATE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_MUNCHAUSEN;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_REGISTRATOR;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.bootstrap.config.GenericConfig;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class ContainerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerConfigService.class);

    @Value("${cb.docker.container.ambari.warm:}")
    private String warmAmbariDockerImageName;

    @Value("${cb.docker.container.ambari:}")
    private String ambariDockerImageName;

    @Value("${cb.docker.container.registrator:" + CB_DOCKER_CONTAINER_REGISTRATOR + "}")
    private String registratorDockerImageName;

    @Value("${cb.docker.container.docker.consul.watch.plugn:" + CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN + "}")
    private String consulWatchPlugnDockerImageName;

    @Value("${cb.docker.container.ambari.db:" + CB_DOCKER_CONTAINER_AMBARI_DB + "}")
    private String postgresDockerImageName;

    @Value("${cb.docker.container.kerberos:" + CB_DOCKER_CONTAINER_KERBEROS + "}")
    private String kerberosDockerImageName;

    @Value("${cb.docker.container.baywatch.server:" + CB_DOCKER_CONTAINER_BAYWATCH_SERVER + "}")
    private String baywatchServerDockerImageName;

    @Value("${cb.docker.container.baywatch.client:" + CB_DOCKER_CONTAINER_BAYWATCH_CLIENT + "}")
    private String baywatchClientDockerImageName;

    @Value("${cb.docker.container.logrotate:" + CB_DOCKER_CONTAINER_LOGROTATE + "}")
    private String logrotateDockerImageName;

    @Value("${cb.docker.container.munchausen:" + CB_DOCKER_CONTAINER_MUNCHAUSEN + "}")
    private String munchausenImageName;

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
                case AMBARI_AGENT:
                    String imageName = getAmbariImageName(stack);
                    config = new GenericConfig.Builder(imageName).build();
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
                case BAYWATCH_SERVER:
                    config = new GenericConfig.Builder(baywatchServerDockerImageName).build();
                    break;
                case BAYWATCH_CLIENT:
                    config = new GenericConfig.Builder(baywatchClientDockerImageName).build();
                    break;
                case LOGROTATE:
                    config = new GenericConfig.Builder(logrotateDockerImageName).build();
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


    private String getAmbariImageName(Stack stack) {
        String imageName;
        if (stack.getCluster().getAmbariStackDetails() == null) {
            imageName = determineImageName(warmAmbariDockerImageName, CB_DOCKER_CONTAINER_AMBARI_WARMUP);
        } else {
            imageName = determineImageName(ambariDockerImageName, CB_DOCKER_CONTAINER_AMBARI);
        }
        return imageName;
    }

    private String determineImageName(String imageName, String defaultImageName) {
        if (Strings.isNullOrEmpty(imageName)) {
            return defaultImageName;
        }
        return imageName;
    }

}
