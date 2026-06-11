package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;

public record GcpLBTraffics(
        @JsonProperty("trafficPorts") Collection<Integer> trafficPorts,
        @JsonProperty("trafficProtocol") NetworkProtocol trafficProtocol) {
}
