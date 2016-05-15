package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public abstract class BaseSaltJobRunner implements SaltJobRunner {

    private Set<String> target = new HashSet<>();
    private JobId jid;
    private JobState jobState = JobState.NOT_STARTED;

    public BaseSaltJobRunner(Set<String> target) {
        this.target = target;
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
            set.addAll(stringObjectMap.entrySet().stream().map(Map.Entry<String, Object>::getKey).collect(Collectors.toList()));
        }
        return set;
    }

    public Set<String> collectMissingNodes(SaltConnector sc, Set<String> nodes) {
        Set<String> nodesTarget = nodes.stream().map(node -> SaltStates.resolveHostNameToMinionHostName(sc, node)).collect(Collectors.toSet());
        return target.stream().filter(t -> !nodesTarget.contains(t)).collect(Collectors.toSet());
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
