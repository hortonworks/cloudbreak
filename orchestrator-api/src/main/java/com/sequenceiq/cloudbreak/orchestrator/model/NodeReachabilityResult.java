package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.common.orchestration.Node;

public class NodeReachabilityResult {

    private Set<Node> reachableNodes = new HashSet<>();

    private Set<Node> unreachableNodes = new HashSet<>();

    public NodeReachabilityResult(Set<Node> reachableNodes, Set<Node> unreachableNodes) {
        if (reachableNodes != null) {
            this.reachableNodes = reachableNodes;
        }
        if (unreachableNodes != null) {
            this.unreachableNodes = unreachableNodes;
        }
    }

    public Set<Node> getReachableNodes() {
        return reachableNodes;
    }

    public Set<Node> getUnreachableNodes() {
        return unreachableNodes;
    }

    public Set<String> getReachableHosts() {
        return reachableNodes.stream()
                .map(node -> node.getHostname())
                .collect(Collectors.toSet());
    }

    public Map<String, String> getReachableHostsForHostGroups() {
        return reachableNodes.stream().collect(Collectors.toMap(Node::getHostGroup, Node::getHostname));
    }

    public Set<String> getUnreachableHosts() {
        return unreachableNodes.stream()
                .map(node -> node.getHostname())
                .collect(Collectors.toSet());
    }

    public Map<String, String> getUnReachableHostsForHostGroups() {
        return unreachableNodes.stream().collect(Collectors.toMap(Node::getHostGroup, Node::getHostname));
    }

    @Override
    public String toString() {
        return "NodeResult{" +
                "reachableNodes=" + getReachableHosts() +
                ", unreachableNodes=" + getUnreachableHosts() +
                '}';
    }
}