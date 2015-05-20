package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AddInstancesOperation extends AzureOperation<Set<Resource>> {
    private String gateWayUserData;
    private String coreUserData;
    private Integer instanceCount;
    private String instanceGroup;

    private AddInstancesOperation(Builder builder) {
        super(builder);
        this.gateWayUserData = builder.gateWayUserData;
        this.coreUserData = builder.coreUserData;
        this.instanceCount = builder.instanceCount;
        this.instanceGroup = builder.instanceGroup;
    }

    @Override
    protected Set<Resource> doExecute(Stack stack) {
        return getCloudResourceManager().addNewResources(stack, coreUserData, instanceCount, instanceGroup, getAzureResourceBuilderInit());
    }

    public static class Builder extends AzureOperation.Builder {
        private String gateWayUserData;
        private String coreUserData;
        private Integer instanceCount;
        private String instanceGroup;

        public Builder withGateWayUserData(String gateWayUserData) {
            this.gateWayUserData = gateWayUserData;
            return this;
        }

        public Builder withCoreUserData(String coreUserData) {
            this.coreUserData = coreUserData;
            return this;
        }

        public Builder withInstanceCount(Integer instanceCount) {
            this.instanceCount = instanceCount;
            return this;
        }

        public Builder withInstanceGroup(String instanceGroup) {
            this.instanceGroup = instanceGroup;
            return this;
        }

        public AddInstancesOperation build() {
            return new AddInstancesOperation(this);
        }
    }
}
