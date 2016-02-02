package com.sequenceiq.cloudbreak.orchestrator.marathon;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarathonAppBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonAppBootstrap.class);

    private final Marathon client;
    private final App app;

    public MarathonAppBootstrap(Marathon client, App app) {
        this.client = client;
        this.app = app;
    }

    @Override
    public Boolean call() throws Exception {
        Integer desiredTasksCount = app.getInstances();
        App appResponse = client.getApp(this.app.getId()).getApp();
        Integer tasksRunning = appResponse.getTasksRunning();

        if (tasksRunning < desiredTasksCount) {
            String msg = String.format("Marathon container '%s' instance count: '%s', desired instance count: '%s'!", app.getId(), tasksRunning,
                    desiredTasksCount);
            throw new CloudbreakOrchestratorFailedException(msg);
        }

        return true;
    }
}
