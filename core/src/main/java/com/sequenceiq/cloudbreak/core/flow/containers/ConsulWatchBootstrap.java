package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.CONSUL_WATCH;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.sequenceiq.cloudbreak.core.flow.DockerClientUtil;

public class ConsulWatchBootstrap implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulWatchBootstrap.class);

    private static final int PORT = 9998;
    private final DockerClient docker;
    private final String longName;
    private final String privateIp;
    private final Long id;
    private final String imageName;

    public ConsulWatchBootstrap(DockerClient docker, String imageName, String longName, String privateIp, Long id) {
        this.docker = docker;
        this.longName = longName;
        this.privateIp = privateIp;
        this.id = id;
        this.imageName = imageName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info(String.format("Consul watch starting on %s node with %s private ip", longName, privateIp));
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        try {
            String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withEnv(String.format("constraint:node==%s", longName), "TRACE=1")
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", CONSUL_WATCH.getName(), id))
                    .withCmd(String.format("consul://%s:8500", privateIp)));
            DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                    .withRestartPolicy(RestartPolicy.alwaysRestart())
                    .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock"))));
            LOGGER.info(String.format("Consul watch started successfully on node %s.", longName));
            return true;
        } catch (Exception ex) {
            LOGGER.info(String.format("Consul watch failed to start on node %s.", longName));
            throw ex;
        }
    }
}
