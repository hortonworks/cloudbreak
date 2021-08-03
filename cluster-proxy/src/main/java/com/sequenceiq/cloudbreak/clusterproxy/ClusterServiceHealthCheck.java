package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterServiceHealthCheck {

    @JsonProperty
    private final int intervalInSec;

    @JsonProperty
    private final String endpointSuffix;

    @JsonProperty
    private final int timeoutInSec;

    @JsonProperty
    private final int healthyStatusCode;

    @JsonCreator
    public ClusterServiceHealthCheck(int intervalInSec, String endpointSuffix, int timeoutInSec, int healthyStatusCode) {
        this.intervalInSec = intervalInSec;
        this.endpointSuffix = endpointSuffix;
        this.timeoutInSec = timeoutInSec;
        this.healthyStatusCode = healthyStatusCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClusterServiceHealthCheck that = (ClusterServiceHealthCheck) o;

        return intervalInSec == that.intervalInSec &&
                Objects.equals(endpointSuffix, that.endpointSuffix) &&
                timeoutInSec == that.timeoutInSec &&
                healthyStatusCode == that.healthyStatusCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(intervalInSec, endpointSuffix, timeoutInSec, healthyStatusCode);
    }

    @Override
    public String toString() {
        return "ClusterServiceHealthCheck{" +
                "intervalInSec='" + intervalInSec +
                "', endpointSuffix='" + endpointSuffix +
                "', timeoutInSec='" + timeoutInSec +
                "', healthyStatusCode='" + healthyStatusCode +
                "'}";
    }
}
