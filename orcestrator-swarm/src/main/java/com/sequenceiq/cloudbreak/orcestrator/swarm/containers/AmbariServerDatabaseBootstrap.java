package com.sequenceiq.cloudbreak.orcestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orcestrator.DockerContainer.AMBARI_DB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.sequenceiq.cloudbreak.orcestrator.swarm.DockerClientUtil;

public class AmbariServerDatabaseBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerDatabaseBootstrap.class);

    private final DockerClient docker;
    private final String imageName;

    public AmbariServerDatabaseBootstrap(DockerClient docker, String imageName) {
        this.docker = docker;
        this.imageName = imageName;
    }

    public String call() throws Exception {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setNetworkMode("host");
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());

        String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("POSTGRES_PASSWORD=%s", "bigdata"),
                        String.format("POSTGRES_USER=%s", "ambari"),
                        String.format("constraint:type==gateway"))
                .withHostConfig(hostConfig)
                .withName(AMBARI_DB.getName()));
        DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withNetworkMode("host")
                .withBinds(new Bind("/data/ambari-server/pgsql/data", new Volume("/var/lib/postgresql/data"))));

        LOGGER.info("Database container for Ambari server started successfully");
        return DockerClientUtil.inspectContainer(docker.inspectContainerCmd(containerId)).getNetworkSettings().getIpAddress();
    }
}
