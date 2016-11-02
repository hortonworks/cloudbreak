package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

public abstract class BaseSaltJobRunner implements SaltJobRunner {

    private Set<String> target = new HashSet<>();

    private Set<Node> allNode;

    private JobId jid;

    private JobState jobState = JobState.NOT_STARTED;

    public BaseSaltJobRunner(Set<String> target, Set<Node> allNode) {
        this.target = target;
        this.allNode = allNode;
    }

    public Set<String> getTarget() {
        return target;
    }

    public void setTarget(Set<String> target) {
        this.target = target;
    }

    public JobId getJid() {
        return jid;
    }

    public void setJid(JobId jid) {
        this.jid = jid;
    }

    public JobState getJobState() {
        return jobState;
    }

    public void setJobState(JobState jobState) {
        this.jobState = jobState;
    }

    public StateType stateType() {
        return StateType.SIMPLE;
    }

    public Set<String> collectNodes(ApplyResponse applyResponse) {
        Set<String> set = new HashSet<>();
        for (Map<String, Object> stringObjectMap : applyResponse.getResult()) {
            set.addAll(stringObjectMap.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        }
        return set;
    }

    public Set<String> collectMissingNodes(Set<String> nodes) {
        Map<String, String> hostNames = allNode.stream().collect(Collectors.toMap(node -> getShortHostName(node.getHostname()), Node::getPrivateIp));
        Set<String> nodesTarget = nodes.stream().map(node -> hostNames.get(getShortHostName(node))).collect(Collectors.toSet());
        return target.stream().filter(t -> !nodesTarget.contains(t)).collect(Collectors.toSet());
    }

    private String getShortHostName(String hostName) {
        return new Scanner(hostName).useDelimiter("\\.").next();
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
