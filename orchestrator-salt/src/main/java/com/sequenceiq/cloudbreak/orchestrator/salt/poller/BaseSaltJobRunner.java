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

    private Set<String> target;

    private final Set<Node> allNode;

    private JobId jid;

    private JobState jobState = JobState.NOT_STARTED;

    private Multimap<String, String> nodesWithError;

    protected BaseSaltJobRunner(Set<String> target, Set<Node> allNode) {
        this.target = target;
        this.allNode = allNode;
    }

    @Override
    public Set<String> getTarget() {
        return target;
    }

    @Override
    public void setTarget(Set<String> target) {
        this.target = target;
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

    public Set<String> collectNodes(ApplyResponse applyResponse) {
        Set<String> set = new HashSet<>();
        for (Map<String, JsonNode> stringObjectMap : applyResponse.getResult()) {
            set.addAll(new ArrayList<>(stringObjectMap.keySet()));
        }
        return set;
    }

    public Set<String> collectMissingNodes(Collection<String> nodes) {
        Map<String, String> hostNames = allNode.stream().collect(Collectors.toMap(node -> getShortHostName(node.getHostname()), Node::getPrivateIp));
        Set<String> nodesTarget = nodes.stream().map(node -> hostNames.get(getShortHostName(node))).collect(Collectors.toSet());
        return target.stream().filter(t -> !nodesTarget.contains(t)).collect(Collectors.toSet());
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
                + "target=" + target
                + ", jid=" + jid
                + ", jobState=" + jobState
                + '}';
    }
}
