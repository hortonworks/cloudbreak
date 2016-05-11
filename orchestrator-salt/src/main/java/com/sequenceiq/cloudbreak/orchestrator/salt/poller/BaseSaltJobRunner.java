package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

public abstract class BaseSaltJobRunner implements SaltJobRunner {

    private Target<String> target;
    private JobId jid;
    private JobState jobState = JobState.NOT_STARTED;

    public BaseSaltJobRunner(Target<String> target) {
        this.target = target;
    }

    public Target<String> getTarget() {
        return target;
    }

    public void setTarget(Target<String> target) {
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
}
