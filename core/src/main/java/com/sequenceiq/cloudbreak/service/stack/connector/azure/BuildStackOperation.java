package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

public class BuildStackOperation extends AzureOperation<Set<Resource>> {
    private String gateWayUserData;
    private String coreUserData;

    private BuildStackOperation(Builder builder) {
        super(builder);
        this.gateWayUserData = builder.gateWayUserData;
        this.coreUserData = builder.coreUserData;
    }

    @Override
    protected Set<Resource> doExecute(Stack stack) {
        return getCloudResourceManager().buildStackResources(stack, gateWayUserData, coreUserData, getAzureResourceBuilderInit());
    }

    public static class Builder extends AzureOperation.Builder {
        private String gateWayUserData;
        private String coreUserData;

        public Builder withGateWayUserData(String gateWayUserData) {
            this.gateWayUserData = gateWayUserData;
            return this;
        }

        public Builder withCoreUserData(String coreUserData) {
            this.coreUserData = coreUserData;
            return this;
        }

        public BuildStackOperation build() {
            return new BuildStackOperation(this);
        }
    }
}
