package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.github.dockerjava.api.model.RestartPolicy.alwaysRestart;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.HAVEGED;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

/**
 * There are two general random devices on Linux: /dev/random and /dev/urandom.
 * The best randomness comes from /dev/random, since it's a blocking device, and
 * will wait until sufficient entropy is available to continue providing output.
 * Assuming your entropy is sufficient, you should see the same quality of
 * randomness from /dev/urandom; however, since it's a non-blocking device,
 * it will continue producing “random” data, even when the entropy pool runs out.
 * This can result in lower quality random data, as repeats of previous data are much more likely.
 * Lots of bad things can happen when the available entropy runs low on a production server,
 * especially when this server performs cryptographic functions.
 */
public class HavegedBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(HavegedBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;

    public HavegedBootstrap(DockerClient docker, String imageName, String nodeName) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Haveged container on: {}", nodeName);

        String name = HAVEGED.getName();
        createContainer(docker, docker.createContainerCmd(imageName)
                        .withRestartPolicy(alwaysRestart())
                        .withPrivileged(true)
                        .withName(name)
                        .withEnv(String.format("constraint:node==%s", nodeName)),
                nodeName);

        startContainer(docker, name);

        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HavegedBootstrap{");
        sb.append("docker=").append(docker);
        sb.append(", imageName='").append(imageName).append('\'');
        sb.append(", nodeName='").append(nodeName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
