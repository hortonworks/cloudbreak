package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeUpdateCheckRequest extends StackEvent {
    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final CloudContext cloudContext;

    private final List<CloudResource> cloudResources;

    public ClusterUpgradeUpdateCheckRequest(Long stackId, CloudStack cloudStack, CloudCredential cloudCredential, CloudContext cloudContext,
            List<CloudResource> cloudResources) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_CLOUDPROVIDER_UPDATE.name(), stackId);
        this.cloudStack = cloudStack;
        this.cloudCredential = cloudCredential;
        this.cloudContext = cloudContext;
        this.cloudResources = cloudResources;
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

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }
}
