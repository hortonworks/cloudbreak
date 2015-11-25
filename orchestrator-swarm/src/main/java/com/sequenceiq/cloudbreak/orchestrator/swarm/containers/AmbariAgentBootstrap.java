package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.github.dockerjava.api.model.RestartPolicy.alwaysRestart;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;

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
                .add("/usr/local/public_host_script.sh", "/etc/ambari-agent/conf/public-hostname.sh")
                .add("/data/jars")
                .addLog(logVolumePath)
                .add(dataVolumes).build();

        String name = String.format("%s-%s", AMBARI_AGENT.getName(), id);
        createContainer(docker, docker.createContainerCmd(imageName)
                .withNetworkMode("host")
                .withRestartPolicy(alwaysRestart())
                .withPrivileged(true)
                .withBinds(binds)
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
