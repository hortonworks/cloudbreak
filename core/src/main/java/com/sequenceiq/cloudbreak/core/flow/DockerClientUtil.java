package com.sequenceiq.cloudbreak.core.flow;

import java.net.SocketTimeoutException;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;

public class DockerClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientUtil.class);

    private static final int MAX_RETRIES = 3;

    public static String createContainer(DockerClient client, CreateContainerCmd createContainerCmd) throws Exception {
        int attempts = 0;
        Exception cause = null;
        String response = null;
        while (attempts < MAX_RETRIES && response == null) {
            try {
                response = createContainerCmd.exec().getId();
            } catch (ProcessingException e) {
                if (e.getCause() instanceof SocketTimeoutException) {
                    LOGGER.info("Read timed out while creating container, checking if container was created....");
                    InspectContainerResponse inspectResponse = inspectContainer(client.inspectContainerCmd(createContainerCmd.getName()));
                    if (inspectResponse!=null && inspectResponse.getId()!=null){
                        response = inspectResponse.getId();
                    } else {
                        attempts++;
                        cause = e;
                    }
                } else {
                    throw e;
                }
            }
        }
        if (response == null) {
            throw cause;
        }
        return response;
    }

    public static void startContainer(DockerClient client, StartContainerCmd startContainerCmd) throws Exception {
        int attempts = 0;
        boolean success = false;
        Exception cause = null;
        while (attempts < MAX_RETRIES && !success) {
            try {
                startContainerCmd.exec();
                success = true;
            } catch (ProcessingException e) {
                if (e.getCause() instanceof SocketTimeoutException) {
                    LOGGER.info("Read timed out while starting container, checking if container is running....");
                    InspectContainerResponse inspectResponse = inspectContainer(client.inspectContainerCmd(startContainerCmd.getContainerId()));
                    if (inspectResponse!=null && inspectResponse.getState().isRunning()){
                        success = true;
                    } else {
                        attempts++;
                        cause = e;
                    }
                } else {
                    throw e;
                }
            }
        }
        if (!success) {
            throw cause;
        }
    }

    public static InspectContainerResponse inspectContainer(InspectContainerCmd inspectContainerCmd) throws Exception {
        int attempts = 0;
        Exception cause = null;
        InspectContainerResponse response = null;
        while (attempts < MAX_RETRIES && response == null) {
            try {
                response = inspectContainerCmd.exec();
            } catch (ProcessingException e) {
                if (e.getCause() instanceof SocketTimeoutException) {
                    LOGGER.error("Couldn't inspect container, trying again.", e);
                    attempts++;
                    cause = e;
                } else {
                    throw e;
                }
            }
        }
        if (response == null) {
            throw cause;
        }
        return response;
    }

}
