package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;

public record GcpLBTraffics(
        Collection<Integer> trafficPorts,
        NetworkProtocol trafficProtocol) {
}
