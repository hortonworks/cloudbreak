package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_AGENT;

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

public class AmbariAgentBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;
    private final Set<String> dataVolumes;
    private final String id;
    private final String cloudPlatform;
    private final DockerClientUtil dockerClientUtil;

    public AmbariAgentBootstrap(DockerClient docker, String imageName, String nodeName, Set<String> dataVolumes, String id,
            String cloudPlatform, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dataVolumes = dataVolumes;
        this.id = id;
        this.cloudPlatform = cloudPlatform;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Ambari agent container.");
        try {
            Bind[] binds = new BindsBuilder()
                    .add("/usr/local/public_host_script.sh", "/etc/ambari-agent/conf/public-hostname.sh")
                    .add("/data/jars")
                    .addLog()
                    .add(dataVolumes).build();

            HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();

            String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", AMBARI_AGENT.getName(), id))
                    .withEnv(String.format("constraint:node==%s", nodeName),
                            String.format("CLOUD_PLATFORM=%s", cloudPlatform),
                            "HADOOP_CLASSPATH=/data/jars/*:/usr/lib/hadoop/lib/*")
                    .withCmd("/start-agent"));

            dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));

            LOGGER.info("Ambari agent container started successfully");
        } catch (Exception ex) {
            LOGGER.error("Ambari agent container failed to start.");
            throw ex;
        }
        return true;
    }

}
