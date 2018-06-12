package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;

@Component
public class AmbariClusterStatusFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusFactory.class);

    private final EnumSet<ClusterStatus> partialStatuses = EnumSet.of(ClusterStatus.INSTALLING, ClusterStatus.INSTALL_FAILED, ClusterStatus.STARTING,
            ClusterStatus.STOPPING);

    private final EnumSet<ClusterStatus> fullStatuses = EnumSet.of(ClusterStatus.INSTALLED, ClusterStatus.STARTED);

    public ClusterStatus createClusterStatus(AmbariClient ambariClient, String blueprint) {
        ClusterStatus clusterStatus;
        if (!isAmbariServerRunning(ambariClient)) {
            clusterStatus = ClusterStatus.AMBARISERVER_NOT_RUNNING;
        } else if (blueprint != null) {
            clusterStatus = determineClusterStatus(ambariClient);
        } else {
            clusterStatus = ClusterStatus.AMBARISERVER_RUNNING;
        }
        return clusterStatus;
    }

    private boolean isAmbariServerRunning(AmbariClient ambariClient) {
        boolean result;
        try {
            result = "RUNNING".equals(ambariClient.healthCheck());
        } catch (Exception ignored) {
            result = false;
        }
        return result;
    }

    private ClusterStatus determineClusterStatus(AmbariClient ambariClient) {
        ClusterStatus clusterStatus;
        try {
            Map<String, List<Integer>> ambariOperations = ambariClient.getRequests("IN_PROGRESS", "PENDING");
            if (!ambariOperations.isEmpty()) {
                clusterStatus = ClusterStatus.PENDING;
            } else {
                Map<String, Map<String, String>> hostComponentsStates = ambariClient.getHostComponentsStates();
                List<String> componentNames = hostComponentsStates.entrySet().stream()
                        .map(Entry::getValue)
                        .map(Map::keySet)
                        .flatMap(Set::stream)
                        .collect(Collectors.toList());
                Map<String, String> componentsCategory = ambariClient.getComponentsCategory(componentNames);

                Map<ClusterStatus, Map<String, String>> clusterStatusMapMap = hostComponentsStates.entrySet().stream()
                        .map(swapMapKeyComponentWithClusterStatus(componentsCategory))
                        .map(this::swapMapKeyHostWithClusterStatus)
                        .map(Map::entrySet)
                        .flatMap(Set::stream)
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (mergeMap, map) -> {
                            mergeMap.putAll(map);
                            return mergeMap;
                        }));

                if (clusterStatusMapMap.keySet().stream().anyMatch(partialStatuses::contains)) {
                    String statusReasonArg = createHostComponentString(partialStatuses, clusterStatusMapMap);
                    clusterStatus = partialStatuses.stream().filter(status -> Objects.nonNull(clusterStatusMapMap.get(status))).findFirst().get();
                    clusterStatus.setStatusReasonArg(statusReasonArg);
                    return clusterStatus;
                } else if (clusterStatusMapMap.keySet().stream().anyMatch(fullStatuses::contains) && clusterStatusMapMap.keySet().size() == 1) {
                    return clusterStatusMapMap.keySet().iterator().next();
                } else {
                    clusterStatus = ClusterStatus.AMBIGUOUS;
                    String statusReasonArg = createHostComponentString(EnumSet.of(ClusterStatus.INSTALLED, ClusterStatus.AMBIGUOUS), clusterStatusMapMap);
                    clusterStatus.setStatusReasonArg(statusReasonArg);
                    return clusterStatus;
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("An error occurred while trying to reach Ambari.", ex);
            clusterStatus = ClusterStatus.UNKNOWN;
        }
        return clusterStatus;
    }

    private String createHostComponentString(EnumSet<ClusterStatus> clusterStatuses, Map<ClusterStatus, Map<String, String>> clusterStatusMapMap) {
        ClusterStatus clusterStatus = clusterStatuses.stream().filter(status -> Objects.nonNull(clusterStatusMapMap.get(status))).findFirst().get();
        return clusterStatusMapMap.get(clusterStatus).entrySet().stream()
                .map(hostComponentMap -> String.join(": ", hostComponentMap.getKey(), hostComponentMap.getValue()))
                .reduce("", reduceToHostComponentList());
    }

    private BinaryOperator<String> reduceToHostComponentList() {
        return (reducedHostComponent, hostComponent) ->
                reducedHostComponent.isEmpty() ? hostComponent : String.join("; ", reducedHostComponent, hostComponent);
    }

    private Function<Entry<String, Map<String, String>>, AbstractMap.SimpleEntry<String, Map<ClusterStatus, String>>> swapMapKeyComponentWithClusterStatus(
            Map<String, String> componentsCategory) {
        return host -> new AbstractMap.SimpleEntry<>(host.getKey(), host.getValue().entrySet().stream()
                .filter(filterClientComponents(componentsCategory))
                .collect(Collectors.toMap(
                        mapToClusterStatusWithDefault(),
                        Entry::getKey,
                        (mergedComponent, component) -> mergedComponent.concat(", ").concat(component))
                )
        );
    }

    private Map<ClusterStatus, Map<String, String>> swapMapKeyHostWithClusterStatus(Entry<String, Map<ClusterStatus, String>> hostStatusMap) {
        return hostStatusMap.getValue().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        createValueAsHostComponentMap(hostStatusMap),
                        mergeHostComponentMapsByValueConcat()
                ));
    }

    private BinaryOperator<Map<String, String>> mergeHostComponentMapsByValueConcat() {
        return (mergehostComponentMap, hostComponentMap) -> {
            hostComponentMap.forEach((host, component) ->
                mergehostComponentMap.merge(host, component, (mergeComponent, conflictingComponent) -> mergeComponent.concat(", ").concat(conflictingComponent))
            );

            return hostComponentMap;
        };
    }

    private Function<Entry<ClusterStatus, String>, Map<String, String>> createValueAsHostComponentMap(Entry<String, Map<ClusterStatus, String>> hostStatusMap) {
        return componentStatus -> {
            Map<String, String> hostComponentMap = new HashMap<>();
            hostComponentMap.put(hostStatusMap.getKey(), componentStatus.getValue());
            return hostComponentMap;
        };
    }

    private Predicate<Entry<String, String>> filterClientComponents(Map<String, String> componentsCategory) {
        return component -> !"CLIENT".equalsIgnoreCase(componentsCategory.get(component.getKey()));
    }

    private Function<Entry<String, String>, ClusterStatus> mapToClusterStatusWithDefault() {
        return component -> {
            if (ClusterStatus.supported(component.getValue())) {
                return ClusterStatus.valueOf(component.getValue());
            } else {
                return ClusterStatus.AMBIGUOUS;
            }
        };
    }
}
