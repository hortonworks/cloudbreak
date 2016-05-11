package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId.jobId;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
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
            LOGGER.info("Job has not started in the cluster. Starting first time.");
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            boolean jobRunning = SaltStates.jobIsRunning(saltConnector, saltJobRunner.getJid().getJobId(), saltJobRunner.getTarget());
            if (jobRunning) {
                LOGGER.info("Job is running currently waiting for next polling attempt.");
                saltJobRunner.setJobState(JobState.IN_PROGRESS);
            } else {
                LOGGER.info("Job finished. Collecting missing nodes");
                checkJobFinishedWithSuccess();
            }
        } else if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            LOGGER.info("Job is running currently checking the current state.");
            boolean jobRunning = SaltStates.jobIsRunning(saltConnector, saltJobRunner.getJid().getJobId(), saltJobRunner.getTarget());
            if (jobRunning) {
                LOGGER.info("Job is running currently waiting for next polling attempt.");
                saltJobRunner.setJobState(JobState.IN_PROGRESS);
            } else {
                LOGGER.info("Job finished. Collecting missing nodes");
                checkJobFinishedWithSuccess();
            }
        } else if (JobState.FAILED.equals(saltJobRunner.getJobState())) {
            LOGGER.info("Job failed in the previous time. Trigger again with these targets: " + saltJobRunner.getTarget());
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
        }
        if (!JobState.FINISHED.equals(saltJobRunner.getJobState())) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from job result: " + saltJobRunner.getTarget());
        }
        LOGGER.info("Job was finished. Triggering next salt event.");
        return true;
    }

    private void checkJobFinishedWithSuccess() {
        Set<String> missingNodes = SaltStates.jidInfo(saltConnector, saltJobRunner.getJid().getJobId(), saltJobRunner.getTarget(),
                saltJobRunner.stateType());
        if (!missingNodes.isEmpty()) {
            LOGGER.info("There are missing nodes after the job completion: " + missingNodes);
            saltJobRunner.setJobState(JobState.FAILED);
            List<String> newTargets = missingNodes.stream().map(node -> SaltStates.resolveHostNameToMinionHostName(saltConnector, node))
                    .collect(Collectors.toList());
            saltJobRunner.setTarget(new Compound(newTargets));
        } else {
            LOGGER.info("There job completed successfuly on every node.");
            saltJobRunner.setJobState(JobState.FINISHED);
        }
    }
}
