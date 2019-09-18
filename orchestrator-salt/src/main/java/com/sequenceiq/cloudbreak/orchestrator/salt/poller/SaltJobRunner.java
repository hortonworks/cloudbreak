package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Set;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SaltJobFailedException;

public interface SaltJobRunner {

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
