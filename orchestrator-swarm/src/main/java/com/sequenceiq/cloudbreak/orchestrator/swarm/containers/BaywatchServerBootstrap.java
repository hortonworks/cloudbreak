package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.BAYWATCH_SERVER;

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
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;


public class BaywatchServerBootstrap implements ContainerBootstrap {

    public static final String CLUSTER_NAME = "hdp-log-cluster";

    private static final Logger LOGGER = LoggerFactory.getLogger(BaywatchServerBootstrap.class);

    private static final int ES_PORT = 9300;
    private static final int ES_TRANSPORT_PORT = 9200;
    private static final int KIBANA_PORT = 3080;
    private static final String ES_WORK_PATH = "/es-work";
    private static final String ES_DATA_PATH = "/es-data";

    private final DockerClient docker;
    private final String imageName;
    private final String node;

    public BaywatchServerBootstrap(DockerClient docker, String imageName, String node) {
        this.docker = docker;
        this.imageName = imageName;
        this.node = node;
    }

    @Override
    public Boolean call() throws Exception {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setNetworkMode("host");
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        Ports ports = new Ports();
        ports.add(new PortBinding(new Ports.Binding(ES_PORT), new ExposedPort(ES_PORT)));
        ports.add(new PortBinding(new Ports.Binding(ES_TRANSPORT_PORT), new ExposedPort(ES_TRANSPORT_PORT)));
        ports.add(new PortBinding(new Ports.Binding(KIBANA_PORT), new ExposedPort(KIBANA_PORT)));
        hostConfig.setPortBindings(ports);
        try {
            String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withExposedPorts(new ExposedPort(ES_PORT), new ExposedPort(ES_TRANSPORT_PORT), new ExposedPort(KIBANA_PORT))
                    .withName(BAYWATCH_SERVER.getName())
                    .withEnv(String.format("constraint:node==%s", node),
                            String.format("ES_CLUSTER_NAME=%s", CLUSTER_NAME),
                            String.format("ES_DATA_PATH=%s", ES_DATA_PATH),
                            String.format("ES_WORK_PATH=%s", ES_WORK_PATH))
                    .withHostConfig(hostConfig));
            DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                    .withPortBindings(
                            new PortBinding(new Ports.Binding("0.0.0.0", ES_PORT), new ExposedPort(ES_PORT)),
                            new PortBinding(new Ports.Binding("0.0.0.0", ES_TRANSPORT_PORT), new ExposedPort(ES_TRANSPORT_PORT)),
                            new PortBinding(new Ports.Binding("0.0.0.0", KIBANA_PORT), new ExposedPort(KIBANA_PORT)))
                    .withBinds(new Bind(ES_DATA_PATH, new Volume(ES_DATA_PATH)),
                            new Bind(ES_WORK_PATH, new Volume(ES_WORK_PATH)))
                    .withNetworkMode("host")
                    .withRestartPolicy(RestartPolicy.alwaysRestart()));
            LOGGER.info("Baywatch server container started successfully");
            return true;
        } catch (Exception ex) {
            LOGGER.info("Baywatch server container failed to start.");
            throw ex;
        }
    }
}
