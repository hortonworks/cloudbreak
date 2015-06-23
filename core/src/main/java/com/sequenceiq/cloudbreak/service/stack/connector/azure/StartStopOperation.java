package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.domain.Stack;

public class StartStopOperation extends AzureOperation<Void> {
    private boolean started;

    private StartStopOperation(Builder builder) {
        super(builder);
        this.started = builder.started;
    }

    @Override
    protected Void doExecute(Stack stack) {
        getCloudResourceManager().startStopResources(stack, started, getAzureResourceBuilderInit());
        return null;
    }

    public static class Builder extends AzureOperation.Builder {
        private boolean started;

        public Builder withStarted(boolean started) {
            this.started = started;
            return this;
        }

        public StartStopOperation build() {
            return new StartStopOperation(this);
        }
    }
}
