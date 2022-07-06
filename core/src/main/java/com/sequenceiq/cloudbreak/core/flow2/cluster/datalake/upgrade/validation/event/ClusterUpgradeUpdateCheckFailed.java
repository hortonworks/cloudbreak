package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeUpdateCheckFailed extends StackEvent {

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final CloudContext cloudContext;

    private final Throwable error;

    @JsonCreator
    public ClusterUpgradeUpdateCheckFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("error") Throwable error) {
        super(ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), stackId);
        this.cloudStack = cloudStack;
        this.cloudCredential = cloudCredential;
        this.cloudContext = cloudContext;
        this.error = error;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public Throwable getError() {
        return error;
    }
}
