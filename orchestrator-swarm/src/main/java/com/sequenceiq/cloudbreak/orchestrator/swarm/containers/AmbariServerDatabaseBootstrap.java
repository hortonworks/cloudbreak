package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_DB;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class AmbariServerDatabaseBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerDatabaseBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;
    private final Set<String> dataVolumes;
    private final DockerClientUtil dockerClientUtil;

    public AmbariServerDatabaseBootstrap(DockerClient docker, String imageName, String nodeName, Set<String> dataVolumes, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dataVolumes = dataVolumes;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {

        Bind[] binds = new BindsBuilder()
                .add("/data/ambari-server/pgsql/data", "/var/lib/postgresql/data")
                .add("/hadoopfs/fs1/logs/consul-watch", "/var/log/consul-watch").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();

        String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("POSTGRES_PASSWORD=%s", "bigdata"),
                        String.format("POSTGRES_USER=%s", "ambari"),
                        String.format("constraint:node==%s", nodeName))
                .withHostConfig(hostConfig)
                .withName(AMBARI_DB.getName()));

        dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));

        LOGGER.info("Database container for Ambari server started successfully");
        return true;
    }

}
