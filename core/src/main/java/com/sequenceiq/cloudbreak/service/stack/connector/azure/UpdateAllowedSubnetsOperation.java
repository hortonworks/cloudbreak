package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.domain.Stack;

public class UpdateAllowedSubnetsOperation extends AzureOperation<Void> {
    private UpdateAllowedSubnetsOperation(Builder builder) {
        super(builder);
    }

    @Override
    protected Void doExecute(Stack stack) {
        getCloudResourceManager().updateAllowedSubnets(stack, getAzureResourceBuilderInit());
        return null;
    }

    public static class Builder extends AzureOperation.Builder {
        public UpdateAllowedSubnetsOperation build() {
            return new UpdateAllowedSubnetsOperation(this);
        }
    }
}
