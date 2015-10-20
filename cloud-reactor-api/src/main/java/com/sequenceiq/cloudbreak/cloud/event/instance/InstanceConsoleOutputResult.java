package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class InstanceConsoleOutputResult {

    private final CloudContext cloudContext;
    private final CloudInstance cloudInstance;
    private final String consoleOutput;

    public InstanceConsoleOutputResult(CloudContext cloudContext, CloudInstance cloudInstance, String consoleOutput) {
        this.cloudContext = cloudContext;
        this.cloudInstance = cloudInstance;
        this.consoleOutput = consoleOutput;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public String getConsoleOutput() {
        return consoleOutput;
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }
}
