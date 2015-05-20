package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.domain.Stack;

public class RollbackOperation extends AzureOperation<Void> {
    private RollbackOperation(Builder builder) {
        super(builder);
    }

    @Override
    protected Void doExecute(Stack stack) {
        getCloudResourceManager().rollbackResources(stack, getAzureResourceBuilderInit());
        return null;
    }

    public static class Builder extends AzureOperation.Builder {
        public RollbackOperation build() {
            return new RollbackOperation(this);
        }
    }
}
