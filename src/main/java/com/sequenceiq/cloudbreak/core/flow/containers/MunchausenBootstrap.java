package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.MUNCHAUSEN;

import java.util.Date;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;

public class MunchausenBootstrap {

    private final DockerClient docker;
    private final String privateIp;
    private final String[] cmd;
    private final String containerName;

    public MunchausenBootstrap(DockerClient docker, String containerName, String privateIp, String[] cmd) {
        this.docker = docker;
        this.privateIp = privateIp;
        this.cmd = cmd;
        this.containerName = containerName;
    }

    public Boolean call() {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        CreateContainerResponse response = docker.createContainerCmd(containerName)
                .withEnv(String.format("BRIDGE_IP=%s", privateIp))
                .withName(MUNCHAUSEN.getName() + new Date().getTime())
                .withHostConfig(hostConfig)
                .withCmd(cmd)
                .exec();
        docker.startContainerCmd(response.getId())
                .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
                .exec();
        return true;
    }
}
