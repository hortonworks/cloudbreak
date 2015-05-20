package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.domain.Stack;

public class DeleteStackOperation extends AzureOperation<Void> {
    private DeleteStackOperation(DeleteStackOperationBuilder builder) {
        super(builder);
    }

    @Override
    protected Void doExecute(Stack stack) {
        getCloudResourceManager().terminateResources(stack, getAzureResourceBuilderInit());
        return null;
    }

    public static class DeleteStackOperationBuilder extends AzureOperation.Builder {
        public DeleteStackOperation build() {
            return new DeleteStackOperation(this);
        }
    }
}
