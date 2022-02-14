package com.sequenceiq.cloudbreak.orchestrator.metadata;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.orchestration.Node;

public class OrchestratorMetadataFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorMetadataFilter.class);

    private final Set<String> includeHosts;

    private final Set<String> excludeHosts;

    private final Set<String> includeHostGroups;

    private final Set<Node> nodes;

    public OrchestratorMetadataFilter(Builder builder) {
        this.includeHosts = builder.includeHosts;
        this.excludeHosts = builder.excludeHosts;
        this.includeHostGroups = builder.includeHostGroups;
        this.nodes = builder.nodes;
    }

    public OrchestratorMetadata apply(OrchestratorMetadata metadata) {
        Set<Node> filteredNodes = CollectionUtils.isNotEmpty(nodes) ? nodes : metadata.getNodes().stream()
                .filter(this::apply)
                .collect(Collectors.toSet());
        return new OrchestratorMetadata(metadata.getGatewayConfigs(), filteredNodes, metadata.getExitCriteriaModel(), metadata.getStack());
    }

    public Set<Node> apply(Set<Node> allNodes) {
        return CollectionUtils.isNotEmpty(nodes) ? nodes : allNodes.stream()
                .filter(this::apply)
                .collect(Collectors.toSet());
    }

    public boolean apply(Node node) {
        return checkExcludeHost(node) && checkIncludeHost(node) && checkIncludeHostgroup(node);
    }

    private boolean checkExcludeHost(Node node) {
        if (CollectionUtils.isNotEmpty(excludeHosts)) {
            LOGGER.debug("Exclude hosts filter: {}", excludeHosts);
            return !nodeHostFilterMatches(node, excludeHosts);
        } else {
            LOGGER.debug("Not found any exclude hosts filter to apply.");
            return true;
        }
    }

    private boolean checkIncludeHost(Node node) {
        if (CollectionUtils.isNotEmpty(includeHosts)) {
            LOGGER.debug("Include hosts filter: {}", includeHosts);
            return nodeHostFilterMatches(node, includeHosts);
        } else {
            LOGGER.debug("Not found any hosts filter to apply.");
            return true;
        }
    }

    private boolean checkIncludeHostgroup(Node node) {
        if (CollectionUtils.isNotEmpty(includeHostGroups)) {
            LOGGER.debug("Include host groups filter: {}", includeHosts);
            return includeHostGroups.contains(node.getHostGroup());
        } else {
            LOGGER.debug("Not found any host groups filter to apply.");
            return true;
        }
    }

    private boolean nodeHostFilterMatches(Node node, Set<String> hosts) {
        return containsIfNotEmpty(hosts, node.getHostname()) || containsIfNotEmpty(hosts, node.getPrivateIp()) || containsIfNotEmpty(hosts, node.getPublicIp());
    }

    private boolean containsIfNotEmpty(Set<String> hosts, String val) {
        return val != null && hosts.contains(val);
    }

    @Override
    public String toString() {
        return "OrchestratorMetadataFilter{" +
                "includeHosts=" + includeHosts +
                ", excludeHosts=" + excludeHosts +
                ", includeHostGroups=" + includeHostGroups +
                ", nodes=" + nodes +
                '}';
    }

    public static class Builder {
        private Set<String> includeHosts;

        private Set<String> excludeHosts;

        private Set<String> includeHostGroups;

        private Set<Node> nodes;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public OrchestratorMetadataFilter build() {
            return new OrchestratorMetadataFilter(this);
        }

        public Builder withNodes(Set<Node> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder includeHosts(Set<String> includeHosts) {
            this.includeHosts = includeHosts;
            return this;
        }

        public Builder includeHostGroups(Set<String> includeHostGroups) {
            this.includeHostGroups = includeHostGroups;
            return this;
        }

        public Builder exlcudeHosts(Set<String> excludeHosts) {
            this.excludeHosts = excludeHosts;
            return this;
        }
    }
}
