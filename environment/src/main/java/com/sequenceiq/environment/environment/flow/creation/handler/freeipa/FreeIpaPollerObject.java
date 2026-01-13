package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class FreeIpaPollerObject {

    private final Long environmentId;

    private final String environmentCrn;

    private final FlowIdentifier flowIdentifier;

    private final Long resourceId;

    public FreeIpaPollerObject(Long environmentId, String environmentCrn, FlowIdentifier flowIdentifier) {
        this(environmentId, environmentCrn, flowIdentifier, null);
    }

    public FreeIpaPollerObject(Long environmentId, String environmentCrn, FlowIdentifier flowIdentifier, Long resourceId) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
        this.resourceId = resourceId;
        this.flowIdentifier = flowIdentifier;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }
}
