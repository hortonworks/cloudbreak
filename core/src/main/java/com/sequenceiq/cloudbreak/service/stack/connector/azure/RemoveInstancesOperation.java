package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Stack;

public class RemoveInstancesOperation extends AzureOperation<Set<String>> {
    private Set<String> origInstanceIds;

    private RemoveInstancesOperation(Builder builder) {
        super(builder);
        this.origInstanceIds = builder.origInstanceIds;
    }

    @Override
    protected Set<String> doExecute(Stack stack) {
        return getCloudResourceManager().removeExistingResources(stack, origInstanceIds, getAzureResourceBuilderInit());
    }

    public static class Builder extends AzureOperation.Builder {
        private Set<String> origInstanceIds;

        public Builder withOrigInstanceIds(Set<String> origInstanceIds) {
            this.origInstanceIds = origInstanceIds;
            return this;
        }

        public RemoveInstancesOperation build() {
            return new RemoveInstancesOperation(this);
        }
    }
}
