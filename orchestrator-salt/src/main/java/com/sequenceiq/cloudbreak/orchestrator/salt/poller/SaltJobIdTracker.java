package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId.jobId;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SaltJobIdTracker implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltJobIdTracker.class);

    private final SaltConnector saltConnector;
    private SaltJobRunner saltJobRunner;

    public SaltJobIdTracker(SaltConnector saltConnector, SaltJobRunner saltJobRunner) {
        this.saltConnector = saltConnector;
        this.saltJobRunner = saltJobRunner;
    }

    @Override
    public Boolean call() throws Exception {
        if (JobState.NOT_STARTED.equals(saltJobRunner.getJobState())) {
            LOGGER.info("Job has not started in the cluster. Starting for first time.");
            JobId jobIdObject = jobId(saltJobRunner.submit(saltConnector));
            String jobId = jobIdObject.getJobId();
            saltJobRunner.setJid(jobIdObject);
            boolean jobRunning = SaltStates.jobIsRunning(saltConnector, jobId, new Compound(saltJobRunner.getTarget()));
            if (jobRunning) {
                LOGGER.info("Job: {} is running currently, waiting for next polling attempt.", jobId);
                saltJobRunner.setJobState(JobState.IN_PROGRESS);
            } else {
                LOGGER.info("Job finished: {}. Collecting missing nodes", jobId);
                checkJobFinishedWithSuccess();
            }
        } else if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} is running currently checking the current state.", jobId);
            boolean jobRunning = SaltStates.jobIsRunning(saltConnector, jobId, new Compound(saltJobRunner.getTarget()));
            if (jobRunning) {
                LOGGER.info("Job: {} is running currently waiting for next polling attempt.", jobId);
                saltJobRunner.setJobState(JobState.IN_PROGRESS);
            } else {
                LOGGER.info("Job: {} finished. Collecting missing nodes", jobId);
                checkJobFinishedWithSuccess();
            }
        } else if (JobState.FAILED.equals(saltJobRunner.getJobState())) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} failed in the previous time. Trigger again with these targets: {}", jobId, saltJobRunner.getTarget());
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
        }
        if (!JobState.FINISHED.equals(saltJobRunner.getJobState())) {
            String jobId = saltJobRunner.getJid().getJobId();
            throw new CloudbreakOrchestratorFailedException(String.format("There are missing nodes from job (jid: %s) result: %s",
                    jobId, saltJobRunner.getTarget()));
        }
        LOGGER.info("Job (jid: {}) was finished. Triggering next salt event.", saltJobRunner.getJid().getJobId());
        return true;
    }

    private void checkJobFinishedWithSuccess() {
        String jobId = saltJobRunner.getJid().getJobId();
        Set<String> missingNodes = SaltStates.jidInfo(saltConnector, jobId, new Compound(saltJobRunner.getTarget()),
                saltJobRunner.stateType());
        if (!missingNodes.isEmpty()) {
            LOGGER.info("There are missing nodes after the job (jid: {}) completion: {}", jobId, missingNodes);
            saltJobRunner.setJobState(JobState.FAILED);
            Set<String> newTargets = missingNodes.stream().map(node -> SaltStates.resolveHostNameToMinionHostName(saltConnector, node))
                    .collect(Collectors.toSet());
            saltJobRunner.setTarget(newTargets);
        } else {
            LOGGER.info("The job (jid: {}) completed successfully on every node.", jobId);
            saltJobRunner.setJobState(JobState.FINISHED);
        }
    }
}
