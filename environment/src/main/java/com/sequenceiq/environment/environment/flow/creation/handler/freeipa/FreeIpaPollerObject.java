package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

public class FreeIpaPollerObject {

    private final Long environmentId;

    private final String environmentCrn;

    private final String flowId;

    private final Long resourceId;

    public FreeIpaPollerObject(Long environmentId, String environmentCrn) {
        this(environmentId, environmentCrn, null, null);
    }

    public FreeIpaPollerObject(Long environmentId, String environmentCrn, String flowId, Long resourceId) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
        this.flowId = flowId;
        this.resourceId = resourceId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public String getFlowId() {
        return flowId;
    }

    public Long getResourceId() {
        return resourceId;
    }
}
