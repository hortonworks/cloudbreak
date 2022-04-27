package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.HostName;

public class HostServiceStatuses {
    private final Map<HostName, HostServiceStatus> hostServiceStates;

    public HostServiceStatuses(Map<HostName, HostServiceStatus> hostServiceStates) {
        this.hostServiceStates = hostServiceStates;
    }

    public boolean anyHostBusy() {
        return hostServiceStates.values().stream().anyMatch(status -> status == HostServiceStatus.BUSY);
    }

    public Set<String> getBusyHosts() {
        return hostServiceStates.entrySet().stream()
                .filter(entry -> entry.getValue() == HostServiceStatus.BUSY)
                .map(Map.Entry::getKey)
                .map(HostName::value)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "HostServiceStatuses{" +
                "hostServiceStates=" + hostServiceStates +
                '}';
    }
}
