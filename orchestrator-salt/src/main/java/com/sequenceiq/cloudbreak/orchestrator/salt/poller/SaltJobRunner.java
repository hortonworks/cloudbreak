package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SaltJobFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public interface SaltJobRunner {

    default void filterForAvailableHosts(SaltConnector saltConnector) {
        try {
            MinionStatusSaltResponse minionStatusSaltResponse = SaltStates.collectNodeStatus(saltConnector);
            List<String> downNodes = minionStatusSaltResponse.downMinions();
            setTargetHostnames(getTargetHostnames().stream()
                    .filter(hostName -> !downNodes.contains(hostName)).collect(Collectors.toSet()));
        } catch (Exception e) {
            setTargetHostnames(Collections.emptySet());
        }
    }

    String submit(SaltConnector saltConnector) throws SaltJobFailedException;

    Set<String> getTargetHostnames();

    void setTargetHostnames(Set<String> newTarget);

    JobId getJid();

    void setJid(JobId jid);

    JobState getJobState();

    void setJobState(JobState jobState);

    Multimap<String, String> getNodesWithError();

    void setNodesWithError(Multimap<String, String> nodesWithError);

    StateType stateType();
}
