package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_AGENT;
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

public class AmbariAgentBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;
    private final Set<String> dataVolumes;
    private final String id;
    private final String cloudPlatform;
    private final LogVolumePath logVolumePath;


    public AmbariAgentBootstrap(DockerClient docker, String imageName, String nodeName, Set<String> dataVolumes, String id,
            String cloudPlatform, LogVolumePath logVolumePath) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dataVolumes = dataVolumes;
        this.id = id;
        this.cloudPlatform = cloudPlatform;
        this.logVolumePath = logVolumePath;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Ambari agent container on: {}", nodeName);

        Bind[] binds = new BindsBuilder()
                .add("/data/jars")
                .addLog(logVolumePath)
                .add(dataVolumes).build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();

        String name = String.format("%s-%s", AMBARI_AGENT.getName(), id);
        createContainer(docker, docker.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withName(name)
                .withEnv(String.format("constraint:node==%s", nodeName),
                        String.format("CLOUD_PLATFORM=%s", cloudPlatform),
                        "HADOOP_CLASSPATH=/data/jars/*:/usr/lib/hadoop/lib/*")
                .withCmd("/start-agent"), nodeName);

        startContainer(docker, name);

        return true;
    }

    @Override
    public String toString() {
        return "AmbariAgentBootstrap{"
                + "imageName='" + imageName + '\''
                + ", nodeName='" + nodeName + '\''
                + '}';
    }

}
