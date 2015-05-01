package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.AMBARI_AGENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.sequenceiq.cloudbreak.core.flow.DockerClientUtil;

public class AmbariAgentBootstrap implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentBootstrap.class);

    private static final int PORT = 49991;
    private final DockerClient docker;
    private final String imageName;
    private final String node;
    private final Set<String> dataVolumes;
    private final String id;

    public AmbariAgentBootstrap(DockerClient docker, String imageName, String node, Set<String> dataVolumes, String id) {
        this.docker = docker;
        this.imageName = imageName;
        this.node = node;
        this.dataVolumes = dataVolumes;
        this.id = id;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Ambari agent container.");
        try {
            HostConfig hostConfig = new HostConfig();
            hostConfig.setNetworkMode("host");
            hostConfig.setPrivileged(true);
            hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());

            Ports ports = new Ports();
            ports.add(new PortBinding(new Ports.Binding(PORT), new ExposedPort(PORT)));
            hostConfig.setPortBindings(ports);

            String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", AMBARI_AGENT.getName(), id))
                    .withEnv(String.format("constraint:node==%s", node),
                            "HADOOP_CLASSPATH=/data/jars/*:/usr/lib/hadoop/lib/*")
                    .withCmd("/start-agent"));
            List<Bind> binds = new ArrayList<>();
            binds.add(new Bind("/usr/local/public_host_script.sh", new Volume("/etc/ambari-agent/conf/public-hostname.sh")));
            binds.add(new Bind("/data/jars", new Volume("/data/jars")));
            for (String volume : dataVolumes) {
                binds.add(new Bind(volume, new Volume(volume)));
            }
            Bind[] array = new Bind[binds.size()];
            binds.toArray(array);
            DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                    .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                    .withNetworkMode("host")
                    .withRestartPolicy(RestartPolicy.alwaysRestart())
                    .withBinds(array));
            LOGGER.info("Ambari agent container started successfully");
            return true;
        } catch (Exception ex) {
            LOGGER.error("Ambari agent container failed to start.");
            throw ex;
        }
    }
}
