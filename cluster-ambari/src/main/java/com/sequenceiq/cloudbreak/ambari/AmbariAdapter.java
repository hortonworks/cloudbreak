package com.sequenceiq.cloudbreak.ambari;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;

@Component
public class AmbariAdapter {

    private  static final String CLIENT_CATEGORY_NAME = "CLIENT";

    private final EnumSet<ClusterStatus> partialStatuses = EnumSet.of(ClusterStatus.INSTALLING, ClusterStatus.INSTALL_FAILED, ClusterStatus.STARTING,
            ClusterStatus.STOPPING);

    private final EnumSet<ClusterStatus> fullStatuses = EnumSet.of(ClusterStatus.INSTALLED, ClusterStatus.STARTED);

    public ClusterStatusResult getClusterStatusHostComponentMap(AmbariClient ambariClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> hostComponentsStates = ambariClient.getHostComponentsStatesCategorized();
        List<ClusterComponent> clusterComponents = objectMapper.convertValue(hostComponentsStates, new TypeReference<List<ClusterComponent>>() { });

        EnumSet<ClusterStatus> clusterStatuses = EnumSet.copyOf(clusterComponents.stream()
                .filter(component -> !CLIENT_CATEGORY_NAME.equalsIgnoreCase(component.category))
                .map(ClusterComponent::getState)
                .collect(Collectors.toList()));

        Optional<ClusterStatus> optPartialStatus = clusterStatuses.stream().filter(partialStatuses::contains).findFirst();
        if (optPartialStatus.isPresent()) {
            ClusterStatus clusterStatus = optPartialStatus.get();
            String componentsInStatus = reduceToHostComponentString(clusterComponents, clusterStatus);
            return new ClusterStatusResult(clusterStatus, componentsInStatus);
        } else if (clusterStatuses.size() == 1 && clusterStatuses.stream().anyMatch(fullStatuses::contains)) {
            ClusterStatus clusterStatus = clusterStatuses.iterator().next();
            String componentsInStatus = reduceToHostComponentString(clusterComponents, clusterStatus);
            return new ClusterStatusResult(clusterStatus, componentsInStatus);
        }

        String componentsInInstalled = reduceToHostComponentString(clusterComponents, ClusterStatus.INSTALLED);
        String componentsInUnsupported = reduceToHostComponentString(clusterComponents, ClusterStatus.AMBIGUOUS);
        return new ClusterStatusResult(ClusterStatus.AMBIGUOUS, String.join("; ", componentsInInstalled, componentsInUnsupported));
    }

    private String reduceToHostComponentString(List<ClusterComponent> clusterComponents, ClusterStatus clusterStatus) {
        return clusterComponents.stream()
                        .filter(component -> clusterStatus.equals(component.state) && !CLIENT_CATEGORY_NAME.equalsIgnoreCase(component.category))
                        .collect(Collectors.toMap(
                                ClusterComponent::getHost,
                                ClusterComponent::getComponentName,
                                (left, right) -> String.join(", ", left, right)
                        )).entrySet().stream()
                        .map(hostComponentEntry -> String.join(": ", hostComponentEntry.getKey(), hostComponentEntry.getValue()))
                        .reduce("", (left, right) ->
                                left.isEmpty() ? right : String.join("; ", left, right)
                        );
    }

    private static class ClusterComponent {

        private final String host;

        private final String componentName;

        private final String category;

        private final ClusterStatus state;

        @JsonCreator
        ClusterComponent(@JsonProperty("host") String host,
                @JsonProperty("component_name") String componentName,
                @JsonProperty("state") String state,
                @JsonProperty("category") String category) {
            this.host = host;
            this.componentName = componentName;
            this.state = ClusterStatus.supported(state) ? ClusterStatus.valueOf(state) : ClusterStatus.AMBIGUOUS;
            this.category = category;
        }

        public String getHost() {
            return host;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getCategory() {
            return category;
        }

        public ClusterStatus getState() {
            return state;
        }
    }

}
