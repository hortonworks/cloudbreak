package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.CONSUL_WATCH;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

public class ConsulWatchBootstrap implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulWatchBootstrap.class);

    private static final int PORT = 9998;
    private final DockerClient docker;
    private final String longName;
    private final String privateIp;
    private final Long id;
    private final String containerName;

    public ConsulWatchBootstrap(DockerClient docker, String containerName, String longName, String privateIp, Long id) {
        this.docker = docker;
        this.longName = longName;
        this.privateIp = privateIp;
        this.id = id;
        this.containerName = containerName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info(String.format("Consul watch starting on %s node with %s private ip", longName, privateIp));
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        try {
            CreateContainerResponse response = docker.createContainerCmd(containerName)
                    .withEnv(String.format("constraint:node==%s", longName), "TRACE=1")
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", CONSUL_WATCH.getName(), id))
                    .withCmd(String.format("consul://%s:8500", privateIp))
                    .exec();
            docker.startContainerCmd(response.getId())
                    .withRestartPolicy(RestartPolicy.alwaysRestart())
                    .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
                    .exec();
            LOGGER.info(String.format("Consul watch start was success on %s node with %s private ip", longName, privateIp));
            return true;
        } catch (Exception ex) {
            LOGGER.info(String.format("Consul watch start failed on %s node with %s private ip", longName, privateIp));
            throw ex;
        }
    }
}
