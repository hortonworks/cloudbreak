package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Status;

@Component
public class AmbariClusterStatusFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusFactory.class);

    private EnumSet<ClusterStatus> partialStatuses = EnumSet.of(ClusterStatus.INSTALLING, ClusterStatus.INSTALL_FAILED, ClusterStatus.STARTING,
            ClusterStatus.STOPPING);
    private EnumSet<ClusterStatus> fullStatuses = EnumSet.of(ClusterStatus.INSTALLED, ClusterStatus.STARTED);

    public AmbariClusterStatus createClusterStatus(AmbariClient ambariClient, String blueprint) {
        ClusterStatus clusterStatus;
        if (!isAmbariServerRunning(ambariClient)) {
            clusterStatus = ClusterStatus.AMBARISERVER_NOT_RUNNING;
        } else if (blueprint != null) {
            clusterStatus = determineClusterStatus(ambariClient, blueprint);
        } else {
            clusterStatus = ClusterStatus.AMBARISERVER_RUNNING;
        }
        return getAmbariClusterStatus(clusterStatus);
    }

    private AmbariClusterStatus getAmbariClusterStatus(ClusterStatus clusterStatus) {
        AmbariClusterStatus ambariClusterStatus;
        switch (clusterStatus) {
            case AMBARISERVER_NOT_RUNNING:
                ambariClusterStatus = null;
                break;
            case AMBARISERVER_RUNNING:
                ambariClusterStatus = new AmbariClusterStatus(clusterStatus, Status.AVAILABLE, null, "Ambari server is running.");
                break;
            case INSTALLED:
                ambariClusterStatus = new AmbariClusterStatus(clusterStatus, Status.AVAILABLE, Status.STOPPED, "Services are installed but not running.");
                break;
            case STARTED:
                ambariClusterStatus = new AmbariClusterStatus(clusterStatus, Status.AVAILABLE, Status.AVAILABLE, "Services are installed and running.");
                break;
            case STARTING:
                ambariClusterStatus = new AmbariClusterStatus(clusterStatus, Status.AVAILABLE, Status.START_IN_PROGRESS, "Services are installed, starting...");
                break;
            case STOPPING:
                ambariClusterStatus = new AmbariClusterStatus(clusterStatus, Status.AVAILABLE, Status.STOP_IN_PROGRESS, "Services are installed, stopping...");
                break;
            default:
                ambariClusterStatus = null;
                break;
        }
        return ambariClusterStatus;
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
            clusterStatus = ClusterStatus.UNKNOWN;
        }
        return clusterStatus;
    }

    private void putComponentState(String componentStateStr, Set<ClusterStatus> orderedPartialStatuses, Set<ClusterStatus> orderedFullStatuses,
            Set<String> unsupportedStatuses) {
        ClusterStatus componentStatus = ClusterStatus.valueOf(componentStateStr);
        if (partialStatuses.contains(componentStatus)) {
            orderedPartialStatuses.add(componentStatus);
        } else if (fullStatuses.contains(componentStatus)) {
            orderedFullStatuses.add(componentStatus);
        } else {
            unsupportedStatuses.add(componentStateStr);
        }
    }
}
