package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;

public class GcpLBTrafficsMap {
    private Multimap<NetworkProtocol, Integer> trafficMap = HashMultimap.create();

    public void addTraffic(NetworkProtocol networkProtocol, Integer port) {
        if (networkProtocol == NetworkProtocol.TCP_UDP) {
            trafficMap.put(NetworkProtocol.TCP, port);
            trafficMap.put(NetworkProtocol.UDP, port);
        } else {
            trafficMap.put(convertProtocol(networkProtocol), port);
        }
    }

    public List<GcpLBTraffics> getTraffics() {
        return trafficMap.asMap().entrySet().stream()
                .map(entry -> new GcpLBTraffics(entry.getValue(), entry.getKey()))
                .toList();
    }

    private NetworkProtocol convertProtocol(NetworkProtocol originalProtocol) {
        return originalProtocol == NetworkProtocol.UDP ? NetworkProtocol.UDP : NetworkProtocol.TCP;
    }
}
