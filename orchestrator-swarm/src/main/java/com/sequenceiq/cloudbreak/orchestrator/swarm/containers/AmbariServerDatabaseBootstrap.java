package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_DB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class AmbariServerDatabaseBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerDatabaseBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String node;

    public AmbariServerDatabaseBootstrap(DockerClient docker, String imageName, String node) {
        this.docker = docker;
        this.imageName = imageName;
        this.node = node;
    }

    @Override
    public Boolean call() throws Exception {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setNetworkMode("host");
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());

        String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("POSTGRES_PASSWORD=%s", "bigdata"),
                        String.format("POSTGRES_USER=%s", "ambari"),
                        String.format("constraint:node==%s", node))
                .withHostConfig(hostConfig)
                .withName(AMBARI_DB.getName()));
        DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withNetworkMode("host")
                .withBinds(new Bind("/data/ambari-server/pgsql/data", new Volume("/var/lib/postgresql/data"))));
        LOGGER.info("Database container for Ambari server started successfully");
        return true;
    }
}
