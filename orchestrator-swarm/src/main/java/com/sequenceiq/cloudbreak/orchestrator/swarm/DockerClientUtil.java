package com.sequenceiq.cloudbreak.orchestrator.swarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;


public class DockerClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientUtil.class);

    private DockerClientUtil() {
    }

    public static void createContainer(DockerClient client, CreateContainerCmd createContainerCmd) throws Exception {
        forceRemove(client, createContainerCmd.getName());
        createContainerCmd.exec();
    }

    public static void forceRemove(DockerClient client, String name) throws Exception {
        try {
            InspectContainerResponse inspectResponse = client.inspectContainerCmd(name).exec();
            if (inspectResponse != null && inspectResponse.getId() != null) {
                LOGGER.warn("Container {} already exists, it will be removed! details: {}.", inspectResponse.getName(), inspectResponse);
                RemoveContainerCmd removeContainerCmd = client.removeContainerCmd(inspectResponse.getId()).withForce(true);
                removeContainerCmd.exec();
                return;
            }
        } catch (NotFoundException ex) {
            return;
        }
    }

    public static void startContainer(DockerClient client, String name) throws Exception {
        client.startContainerCmd(name).exec();
        InspectContainerResponse inspectResponse = client.inspectContainerCmd(name).exec();
        if (inspectResponse == null || !inspectResponse.getState().isRunning()) {
            LOGGER.warn("Container {} failed to start! details: {}.", name, inspectResponse);
            throw new CloudbreakOrchestratorFailedException(String.format("Container %s failed to start! ", name));
        }
    }


}
