package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.MUNCHAUSEN;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;

public class MunchausenBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MunchausenBootstrap.class);

    private final DockerClient docker;
    private final String[] cmd;
    private final String containerName;

    public MunchausenBootstrap(DockerClient docker, String containerName, String[] cmd) {
        this.docker = docker;
        this.cmd = cmd;
        this.containerName = containerName;
    }

    @Override
    public Boolean call() throws Exception {

        Bind[] binds = new BindsBuilder()
                .addDockerSocket().build();

        String name = MUNCHAUSEN.getName() + new Date().getTime();
        createContainer(docker, docker.createContainerCmd(containerName)
                .withName(name)
                .withPrivileged(true)
                .withBinds(binds)
                .withCmd(cmd), name);

        startContainer(docker, name);
        LOGGER.info("Munchausen bootstrap container started.");
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MunchausenBootstrap{");
        sb.append("docker=").append(docker);
        sb.append(", cmd=").append(Arrays.toString(cmd));
        sb.append(", containerName='").append(containerName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
