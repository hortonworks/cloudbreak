package com.sequenceiq.it.cloudbreak.util.azure;

public class AzureInstanceActionResult {

    private final boolean success;

    private final String instanceState;

    private final String instanceId;

    public AzureInstanceActionResult(boolean success, String instanceState, String instanceId) {
        this.success = success;
        this.instanceState = instanceState;
        this.instanceId = instanceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getInstanceState() {
        return instanceState;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
