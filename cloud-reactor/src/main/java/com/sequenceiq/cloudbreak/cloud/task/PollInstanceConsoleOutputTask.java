package com.sequenceiq.cloudbreak.cloud.task;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollInstanceConsoleOutputTask extends PollTask<InstanceConsoleOutputResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollInstanceConsoleOutputTask.class);
    private static final String CB_FINGERPRINT_END = "-----END SSH HOST KEY FINGERPRINTS-----";

    private final CloudInstance instance;

    public PollInstanceConsoleOutputTask(CloudConnector connector, AuthenticatedContext authenticatedContext, CloudInstance instance) {
        super(connector, authenticatedContext);
        this.instance = instance;
    }

    @Override
    public InstanceConsoleOutputResult call() throws Exception {
        LOGGER.info("Get console output of instance: {}, for stack: {}.", instance.getInstanceId(), getAuthenticatedContext().getCloudContext().getStackName());
        String consoleOutput = getConnector().instances().getConsoleOutput(getAuthenticatedContext(), instance);
        return new InstanceConsoleOutputResult(getAuthenticatedContext().getCloudContext(), instance, consoleOutput);
    }

    @Override
    public boolean completed(InstanceConsoleOutputResult instanceConsoleOutputResult) {
        return instanceConsoleOutputResult.getConsoleOutput().contains(CB_FINGERPRINT_END);
    }
}
