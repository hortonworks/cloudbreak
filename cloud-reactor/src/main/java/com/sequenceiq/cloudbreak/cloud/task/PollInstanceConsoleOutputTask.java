package com.sequenceiq.cloudbreak.cloud.task;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.handler.GetSSHFingerprintsHandler;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

@Component(PollInstanceConsoleOutputTask.NAME)
@Scope(value = "prototype")
public class PollInstanceConsoleOutputTask extends AbstractPollTask<InstanceConsoleOutputResult> {
    public static final String NAME = "pollInstanceConsoleOutputTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(PollInstanceConsoleOutputTask.class);
    private static final String CB_FINGERPRINT_END = "-----END SSH HOST KEY FINGERPRINTS-----";

    private final CloudInstance instance;
    private final InstanceConnector instanceConnector;

    public PollInstanceConsoleOutputTask(InstanceConnector instanceConnector, AuthenticatedContext authenticatedContext, CloudInstance instance) {
        super(authenticatedContext);
        this.instanceConnector = instanceConnector;
        this.instance = instance;
    }

    @Override
    public InstanceConsoleOutputResult call() throws Exception {
        LOGGER.info("Get console output of instance: {}, for stack: {}.", instance.getInstanceId(), getAuthenticatedContext().getCloudContext().getName());
        String consoleOutput = instanceConnector.getConsoleOutput(getAuthenticatedContext(), instance);
        return new InstanceConsoleOutputResult(getAuthenticatedContext().getCloudContext(), instance, consoleOutput);
    }

    @Override
    public boolean completed(InstanceConsoleOutputResult instanceConsoleOutputResult) {
        String output = instanceConsoleOutputResult.getConsoleOutput();
        boolean contains = output.contains(CB_FINGERPRINT_END);
        if (contains) {
            return true;
        }
        Set<String> fingerprints = GetSSHFingerprintsHandler.FingerprintParserUtil.parseFingerprints(output);
        return !fingerprints.isEmpty();
    }
}
