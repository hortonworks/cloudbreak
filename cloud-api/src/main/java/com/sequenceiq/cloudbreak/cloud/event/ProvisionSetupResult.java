package com.sequenceiq.cloudbreak.cloud.event;

import java.util.Map;

public class ProvisionSetupResult {

    private Map<String, Object> setupProperties;
    private Exception exception;

    public ProvisionSetupResult(Map<String, Object> setupProperties) {
        this.setupProperties = setupProperties;
    }

    public ProvisionSetupResult(Exception exception) {
        this.exception = exception;
    }

    public Map<String, Object> getSetupProperties() {
        return setupProperties;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isFailed() {
        return exception != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ProvisionSetupResult{");
        sb.append("setupProperties=").append(setupProperties);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
