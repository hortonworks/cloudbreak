package com.sequenceiq.cloudbreak.cloud.event;

public class PreProvisionCheckResult {

    private final String message;

    public PreProvisionCheckResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PreProvisionCheckResult{");
        sb.append("message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
