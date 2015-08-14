package com.sequenceiq.cloudbreak.cloud.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class PollInstanceConsoleOutputTask implements PollTask<InstanceConsoleOutputResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollInstanceConsoleOutputTask.class);
    private static final String CB_FINGERPRINT_END = "-----END SSH HOST KEY FINGERPRINTS-----";

    private final CloudConnector connector;
    private final AuthenticatedContext authenticatedContext;
    private final CloudInstance instance;

    public PollInstanceConsoleOutputTask(CloudConnector connector, AuthenticatedContext authenticatedContext, CloudInstance instance) {
        this.connector = connector;
        this.authenticatedContext = authenticatedContext;
        this.instance = instance;
    }

    @Override
    public InstanceConsoleOutputResult call() throws Exception {
        LOGGER.info("Get console output of instance: {}, for stack: {}.", instance.getInstanceId(), authenticatedContext.getCloudContext().getStackName());
        String consoleOutput = connector.instances().getConsoleOutput(authenticatedContext, instance);
        return new InstanceConsoleOutputResult(authenticatedContext.getCloudContext(), instance, consoleOutput);
    }

    @Override
    public boolean completed(InstanceConsoleOutputResult instanceConsoleOutputResult) {
        return instanceConsoleOutputResult.getConsoleOutput().contains(CB_FINGERPRINT_END);
    }
}
