package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.domain.Stack;

public class StartStopOperation extends AzureOperation<Boolean> {
    private boolean started;

    private StartStopOperation(Builder builder) {
        super(builder);
        this.started = builder.started;
    }

    @Override
    protected Boolean doExecute(Stack stack) {
        return getCloudResourceManager().startStopResources(stack, started, getAzureResourceBuilderInit());
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
