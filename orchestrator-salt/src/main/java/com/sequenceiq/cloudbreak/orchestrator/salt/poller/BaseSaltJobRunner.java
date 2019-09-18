package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

public abstract class BaseSaltJobRunner implements SaltJobRunner {

    private Set<String> targetHostnames;

    private final Set<Node> allNode;

    private JobId jid;

    private JobState jobState = JobState.NOT_STARTED;

    private Multimap<String, String> nodesWithError;

    protected BaseSaltJobRunner(Set<String> targetHostnames, Set<Node> allNode) {
        this.targetHostnames = targetHostnames;
        this.allNode = allNode;
    }

    @Override
    public Set<String> getTargetHostnames() {
        return targetHostnames;
    }

    @Override
    public void setTargetHostnames(Set<String> targetHostnames) {
        this.targetHostnames = targetHostnames;
    }

    @Override
    public JobId getJid() {
        return jid;
    }

    @Override
    public void setJid(JobId jid) {
        this.jid = jid;
    }

    @Override
    public JobState getJobState() {
        return jobState;
    }

    @Override
    public void setJobState(JobState jobState) {
        this.jobState = jobState;
    }

    @Override
    public Multimap<String, String> getNodesWithError() {
        return nodesWithError;
    }

    @Override
    public void setNodesWithError(Multimap<String, String> nodesWithError) {
        this.nodesWithError = nodesWithError;
    }

    @Override
    public StateType stateType() {
        return StateType.SIMPLE;
    }

    public Set<String> collectSucceededNodes(ApplyResponse applyResponse) {
        Set<String> set = new HashSet<>();
        for (Map<String, JsonNode> stringObjectMap : applyResponse.getResult()) {
            set.addAll(new ArrayList<>(stringObjectMap.keySet()));
        }
        return set;
    }

    public Set<String> collectMissingHostnames(Collection<String> succeededHostnames) {
        Map<String, String> shortHostnameToHostnameMap =
                allNode.stream().collect(Collectors.toMap(node -> getShortHostName(node.getHostname()), Node::getHostname));
        Set<String> resolvedSucceededHostnames =
                succeededHostnames.stream().map(hostname -> shortHostnameToHostnameMap.get(getShortHostName(hostname))).collect(Collectors.toSet());
        return targetHostnames.stream().filter(t -> !resolvedSucceededHostnames.contains(t)).collect(Collectors.toSet());
    }

    protected Set<Node> getAllNode() {
        return allNode;
    }

    private String getShortHostName(String hostName) {
        try (Scanner scanner = new Scanner(hostName)) {
            try (Scanner delimiter = scanner.useDelimiter("\\.")) {
                return delimiter.next();
            }
        }
    }

    @Override
    public String toString() {
        return "BaseSaltJobRunner{"
                + "targetHostnames=" + targetHostnames
                + ", jid=" + jid
                + ", jobState=" + jobState
                + '}';
    }
}
