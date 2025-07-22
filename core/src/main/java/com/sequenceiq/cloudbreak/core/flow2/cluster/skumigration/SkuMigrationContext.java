package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class SkuMigrationContext extends CommonContext {

    private final StackView stack;

    private final String cloudPlatform;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudConnector cloudConnector;

    private final CloudStack cloudStack;

    private final Set<ProviderSyncState> providerSyncStates;

    public SkuMigrationContext(FlowParameters flowParameters, StackView stack, String cloudPlatform, CloudContext cloudContext,
            CloudCredential cloudCredential, CloudConnector cloudConnector, CloudStack cloudStack, Set<ProviderSyncState> providerSyncStates) {
        super(flowParameters);
        this.stack = stack;
        this.cloudPlatform = cloudPlatform;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudConnector = cloudConnector;
        this.cloudStack = cloudStack;
        this.providerSyncStates = providerSyncStates;
    }

    public StackView getStack() {
        return stack;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudConnector getCloudConnector() {
        return cloudConnector;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public Set<ProviderSyncState> getProviderSyncStates() {
        return providerSyncStates;
    }
}
