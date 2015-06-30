package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;

@Component
public class AmbariClusterStatusFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusFactory.class);

    private EnumSet<ClusterStatus> partialStatuses = EnumSet.of(ClusterStatus.INSTALLING, ClusterStatus.INSTALL_FAILED, ClusterStatus.STARTING,
            ClusterStatus.STOPPING);
    private EnumSet<ClusterStatus> fullStatuses = EnumSet.of(ClusterStatus.INSTALLED, ClusterStatus.STARTED);

    public ClusterStatus createClusterStatus(AmbariClient ambariClient, String blueprint) {
        ClusterStatus clusterStatus;
        if (!isAmbariServerRunning(ambariClient)) {
            clusterStatus = ClusterStatus.AMBARISERVER_NOT_RUNNING;
        } else if (blueprint != null) {
            clusterStatus = determineClusterStatus(ambariClient, blueprint);
        } else {
            clusterStatus = ClusterStatus.AMBARISERVER_RUNNING;
        }
        return clusterStatus;
    }

    private boolean isAmbariServerRunning(AmbariClient ambariClient) {
        boolean result;
        try {
            result = "RUNNING".equals(ambariClient.healthCheck());
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    private ClusterStatus determineClusterStatus(AmbariClient ambariClient, String blueprint) {
        ClusterStatus clusterStatus;
        try {
            Map<String, List<Integer>> ambariOperations = ambariClient.getRequests("IN_PROGRESS", "PENDING");
            if (!ambariOperations.isEmpty()) {
                clusterStatus = ClusterStatus.PENDING;
            } else {
                Set<ClusterStatus> orderedPartialStatuses = new TreeSet<>();
                Set<ClusterStatus> orderedFullStatuses = new TreeSet<>();
                Set<String> unsupportedStatuses = new HashSet<>();
                Map<String, String> componentsCategory = ambariClient.getComponentsCategory(blueprint);
                Map<String, Map<String, String>> hostComponentsStates = ambariClient.getHostComponentsStates();
                for (Map.Entry<String, Map<String, String>> hostComponentsEntry : hostComponentsStates.entrySet()) {
                    Map<String, String> componentStateMap = hostComponentsEntry.getValue();
                    for (Map.Entry<String, String> componentStateEntry : componentStateMap.entrySet()) {
                        String category = componentsCategory.get(componentStateEntry.getKey());
                        if (!"CLIENT".equals(category)) {
                            putComponentState(componentStateEntry.getValue(), orderedPartialStatuses, orderedFullStatuses, unsupportedStatuses);
                        }
                    }
                }
                clusterStatus = determineClusterStatus(orderedPartialStatuses, orderedFullStatuses);
            }
        } catch (Exception ex) {
            LOGGER.warn("There was a problem with the ambari.");
            clusterStatus = ClusterStatus.UNKNOWN;
        }
        return clusterStatus;
    }

    private ClusterStatus determineClusterStatus(Set<ClusterStatus> orderedPartialStatuses, Set<ClusterStatus> orderedFullStatuses) {
        ClusterStatus clusterStatus;
        if (!orderedPartialStatuses.isEmpty()) {
            clusterStatus = orderedPartialStatuses.iterator().next();
        } else if (orderedFullStatuses.size() == 1) {
            clusterStatus = orderedFullStatuses.iterator().next();
        } else {
            clusterStatus = ClusterStatus.AMBIGUOUS;
        }
        return clusterStatus;
    }

    private void putComponentState(String componentStateStr, Set<ClusterStatus> orderedPartialStatuses, Set<ClusterStatus> orderedFullStatuses,
            Set<String> unsupportedStatuses) {
        try {
            ClusterStatus componentStatus = ClusterStatus.valueOf(componentStateStr);
            if (partialStatuses.contains(componentStatus)) {
                orderedPartialStatuses.add(componentStatus);
            } else if (fullStatuses.contains(componentStatus)) {
                orderedFullStatuses.add(componentStatus);
            } else {
                unsupportedStatuses.add(componentStateStr);
            }
        } catch (RuntimeException ex) {
            unsupportedStatuses.add(componentStateStr);
        }
    }
}
