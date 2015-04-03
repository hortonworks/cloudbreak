package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.AMBARI_DB;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

public class AmbariServerDatabaseBootstrap {

    private final DockerClient docker;
    private final String containerName;

    public AmbariServerDatabaseBootstrap(DockerClient docker, String containerName) {
        this.docker = docker;
        this.containerName = containerName;
    }

    public String call() throws Exception {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());

        CreateContainerResponse response = docker.createContainerCmd(containerName)
                .withEnv(String.format("POSTGRES_PASSWORD=%s", "bigdata"),
                        String.format("POSTGRES_USER=%s", "ambari"),
                        String.format("constraint:type==gateway"))
                .withHostConfig(hostConfig)
                .withName(AMBARI_DB.getName())
                .exec();
        docker.startContainerCmd(response.getId())
                .withBinds(new Bind("/data/ambari-server/pgsql/data", new Volume("/var/lib/postgresql/data")))
                .exec();

        InspectContainerCmd inspectContainerCmd = docker.inspectContainerCmd(response.getId());
        return inspectContainerCmd.exec().getNetworkSettings().getIpAddress();
    }
}
