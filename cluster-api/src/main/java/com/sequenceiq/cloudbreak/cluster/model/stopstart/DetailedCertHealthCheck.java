package com.sequenceiq.cloudbreak.cluster.model.stopstart;

import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;

/**
 * DetailedCertHealthCheck maintains information about certificate health, along with healthCheck explanation returned by CM
 */
public class DetailedCertHealthCheck {

    private final HealthCheckResult healthCheckResult;

    private final String explanation;

    public DetailedCertHealthCheck(HealthCheckResult healthCheckResult, String explanation) {
        this.healthCheckResult = healthCheckResult;
        this.explanation = explanation;
    }

    public HealthCheckResult getHealthCheckResult() {
        return healthCheckResult;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return "DetailedCertHealthCheck{" +
                "healthCheckType=" + healthCheckResult +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
