package com.sequenceiq.cloudbreak.common.type;

import java.util.List;
import java.util.Optional;

public class HealthCheck {

    private final HealthCheckType type;

    private final HealthCheckResult result;

    private final Optional<String> reason;

    private final List<String> details;

    public HealthCheck(HealthCheckType type, HealthCheckResult result, Optional<String> reason, List<String> details) {
        this.type = type;
        this.result = result;
        this.reason = reason;
        this.details = details;
    }

    public static HealthCheck healthy(HealthCheckType type) {
        return new HealthCheck(type, HealthCheckResult.HEALTHY, Optional.empty(), List.of());
    }

    public static HealthCheck unhealthy(HealthCheckType type) {
        return new HealthCheck(type, HealthCheckResult.UNHEALTHY, Optional.empty(), List.of());
    }

    public static HealthCheck unhealthy(HealthCheckType type, String reason) {
        return new HealthCheck(type, HealthCheckResult.UNHEALTHY, Optional.ofNullable(reason), List.of());
    }

    public static HealthCheck unhealthy(HealthCheckType type, String reason, List<String> details) {
        return new HealthCheck(type, HealthCheckResult.UNHEALTHY, Optional.ofNullable(reason), details);
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

    public List<String> getDetails() {
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
