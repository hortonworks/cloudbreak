package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class AmbariServerDatabaseBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerDatabaseBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;
    private final Set<String> dataVolumes;
    private final LogVolumePath logVolumePath;

    public AmbariServerDatabaseBootstrap(DockerClient docker, String imageName, String nodeName, Set<String> dataVolumes,
            LogVolumePath logVolumePath) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dataVolumes = dataVolumes;
        this.logVolumePath = logVolumePath;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Ambari db container on: {}", nodeName);

        Bind[] binds = new BindsBuilder()
                .add("/data/ambari-server/pgsql/data", "/var/lib/postgresql/data")
                .add(logVolumePath.getHostPath() + "/consul-watch",
                        logVolumePath.getHostPath() + "/consul-watch").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();

        String name = AMBARI_DB.getName();
        createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("POSTGRES_PASSWORD=%s", "bigdata"),
                        String.format("POSTGRES_USER=%s", "ambari"),
                        String.format("constraint:node==%s", nodeName))
                .withHostConfig(hostConfig)
                .withName(name));

        startContainer(docker, name);

        return true;
    }

}
