package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.model.Info;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.core.flow.context.DockerContext;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class DockerCheckerTask extends StackBasedStatusCheckerTask<DockerContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerCheckerTask.class);

    @Override
    public boolean checkStatus(DockerContext dockerContext) {
        LOGGER.info("Checking docker if available.");
        try {
            Info info = dockerContext.getDockerClient().infoCmd().exec();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void handleTimeout(DockerContext t) {
        throw new InternalServerException(String.format("Operation timed out. Could not reach docker in time."));
    }

    @Override
    public String successMessage(DockerContext t) {
        return String.format("Docker available successfully ");
    }
}
