package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeCredentialValidationFailed extends StackEvent {
    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final CloudContext cloudContext;

    private final Throwable error;

    public ClusterUpgradeCredentialValidationFailed(Long stackId, CloudStack cloudStack, CloudCredential cloudCredential, CloudContext cloudContext,
            Throwable error) {
        super(stackId);
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
