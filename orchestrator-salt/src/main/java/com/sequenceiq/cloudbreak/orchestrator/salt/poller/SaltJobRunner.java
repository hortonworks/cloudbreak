package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

public interface SaltJobRunner {

    String submit(SaltConnector saltConnector);

    Set<String> getTarget();

    void setTarget(Set<String> newTarget);

    JobId getJid();

    void setJid(JobId jid);

    JobState getJobState();

    void setJobState(JobState jobState);

    StateType stateType();
}
