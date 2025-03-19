package com.sequenceiq.cloudbreak.common.type;

import java.util.Optional;

public class HealthCheck {

    private final HealthCheckType type;

    private final HealthCheckResult result;

    private final Optional<String> reason;

    private final Optional<String> details;

    public HealthCheck(HealthCheckType type, HealthCheckResult result, Optional<String> reason, Optional<String> details) {
        this.type = type;
        this.result = result;
        this.reason = reason;
        this.details = details;
    }

    public HealthCheckType getType() {
        return type;
    }

    public HealthCheckResult getResult() {
        return result;
    }

    public Optional<String> getReason() {
        return reason;
    }

    public Optional<String> getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "HealthCheck{" +
                "type=" + type +
                ", result=" + result +
                ", reason=" + reason +
                ", details=" + details +
                '}';
    }
}
