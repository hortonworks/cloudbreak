package com.sequenceiq.cloudbreak.orchestrator.swarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;


public class DockerClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientUtil.class);

    private DockerClientUtil() {
    }

    public static void createContainer(DockerClient client, CreateContainerCmd cmd, String node) {
        String name = cmd.getName();
        try {
            InspectContainerResponse inspectResponse = inspect(client, name);
            if (inspectResponse != null && inspectResponse.getId() != null && !isContainerRunning(inspectResponse)) {
                remove(client, inspectResponse, name, node);
            }
        } catch (NotFoundException ex) {
            create(cmd, node, name);
        }
    }

    public static void startContainer(DockerClient client, String name) throws Exception {
        try {
            start(client, name);
            InspectContainerResponse inspectResponse = inspect(client, name);
            if (inspectResponse == null || !isContainerRunning(inspectResponse)) {
                LOGGER.warn("Container {} failed to start! details: {}.", name, inspectResponse);
                throw new CloudbreakOrchestratorFailedException(String.format("Container %s failed to start! ", name));
            }
        } catch (NotModifiedException e) {
            LOGGER.info("Container {} is already running.", name);
        }
    }

    public static void remove(DockerClient client, String name, String node) throws CloudbreakOrchestratorFailedException {
        try {
            InspectContainerResponse inspectResponse = inspect(client, name);
            if (inspectResponse != null && inspectResponse.getId() != null && isContainerRunning(inspectResponse)) {
                LOGGER.warn("Container {} is still running on node: {}! Trying to remove it again.", name, node);
                remove(client, inspectResponse, name, node);
            }
            throw new CloudbreakOrchestratorFailedException(String.format("Container {} is still running on node: {}!", name, node));
        } catch (NotFoundException ex) {
            LOGGER.info("Container '{}' has already been deleted from node '{}'.", name, node);
        }
    }

    private static void start(DockerClient client, String name) {
        long start = System.currentTimeMillis();
        client.startContainerCmd(name).exec();
        LOGGER.info("Container {} start command took {} ms", name, System.currentTimeMillis() - start);
    }

    private static InspectContainerResponse inspect(DockerClient client, String name) {
        long start = System.currentTimeMillis();
        InspectContainerResponse inspectResponse = client.inspectContainerCmd(name).exec();
        LOGGER.info("Container {} inspect command took {} ms", name, System.currentTimeMillis() - start);
        return inspectResponse;
    }

    private static void remove(DockerClient client, InspectContainerResponse inspectResponse, String name, String node) {
        LOGGER.warn("Container {} already exists, it will be removed! node: {}", name, node);
        long start = System.currentTimeMillis();
        client.removeContainerCmd(inspectResponse.getId()).withForce(true).exec();
        LOGGER.info("Container {} remove command took {} ms", name, System.currentTimeMillis() - start);
    }

    private static void create(CreateContainerCmd cmd, String node, String name) {
        LOGGER.info("Creating container {} on node {}", name, node);
        long start = System.currentTimeMillis();
        cmd.exec();
        LOGGER.info("Container {} create command took {} ms", name, System.currentTimeMillis() - start);
    }

    private static boolean isContainerRunning(InspectContainerResponse inspect) {
        return inspect.getState().isRunning();
    }

}
