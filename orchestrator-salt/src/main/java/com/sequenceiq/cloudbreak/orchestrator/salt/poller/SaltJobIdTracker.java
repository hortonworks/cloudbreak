package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId.jobId;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorInProgressException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTerminateException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SaltJobIdTracker implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltJobIdTracker.class);

    private final SaltConnector saltConnector;

    private final SaltJobRunner saltJobRunner;

    private final boolean retryOnFail;

    public SaltJobIdTracker(SaltConnector saltConnector, SaltJobRunner saltJobRunner) {
        this(saltConnector, saltJobRunner, true);
    }

    public SaltJobIdTracker(SaltConnector saltConnector, SaltJobRunner saltJobRunner, boolean retryOnFail) {
        this.saltConnector = saltConnector;
        this.saltJobRunner = saltJobRunner;
        this.retryOnFail = retryOnFail;
    }

    @Override
    public Boolean call() throws Exception {
        if (JobState.NOT_STARTED.equals(saltJobRunner.getJobState())) {
            LOGGER.info("Job has not started in the cluster. Starting for first time.");
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            checkIsFinished(saltJobRunner.getJid().getJobId());
        } else if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} is running currently checking the current state.", jobId);
            checkIsFinished(jobId);
        } else if (!retryOnFail && JobState.FAILED == saltJobRunner.getJobState()) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} failed. Terminate execution on these targets: {}", jobId, saltJobRunner.getTargetHostnames());
            throw new CloudbreakOrchestratorTerminateException(buildErrorMessage(), saltJobRunner.getNodesWithError());
        } else if (JobState.FAILED == saltJobRunner.getJobState() || JobState.AMBIGUOUS == saltJobRunner.getJobState()) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} failed in the previous time. Trigger again with these targets: {}", jobId, saltJobRunner.getTargetHostnames());
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
            return call();
        }
        if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            String jobIsRunningMessage = String.format("Job: %s is running currently. Details: %s", saltJobRunner.getJid(), buildErrorMessage());
            throw new CloudbreakOrchestratorInProgressException(jobIsRunningMessage, saltJobRunner.getNodesWithError());
        }
        if (JobState.FAILED == saltJobRunner.getJobState() || JobState.AMBIGUOUS == saltJobRunner.getJobState()) {
            throw new CloudbreakOrchestratorFailedException(buildErrorMessage(), saltJobRunner.getNodesWithError());
        }
        LOGGER.info("Job (jid: {}) was finished. Triggering next salt event.", saltJobRunner.getJid().getJobId());
        return true;
    }

    private void checkIsFinished(String jobId) throws CloudbreakOrchestratorFailedException {
        boolean jobRunning = SaltStates.jobIsRunning(saltConnector, jobId);
        if (jobRunning) {
            LOGGER.info("Job: {} is running currently, waiting for next polling attempt.", jobId);
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
        } else {
            LOGGER.info("Job finished: {}. Collecting missing nodes", jobId);
            checkJobFinishedWithSuccess();
        }
    }

    private String buildErrorMessage() {
        String jobId = saltJobRunner.getJid().getJobId();
        StringBuilder errorMessageBuilder = new StringBuilder();
        errorMessageBuilder.append(String.format("There are missing nodes from job (jid: %s), target: %s", jobId, saltJobRunner.getTargetHostnames()));
        if (saltJobRunner.getNodesWithError() != null) {
            for (String host : saltJobRunner.getNodesWithError().keySet()) {
                Collection<String> errorMessages = saltJobRunner.getNodesWithError().get(host);
                errorMessageBuilder.append('\n').append("Node: ").append(host).append(" Error(s): ").append(String.join(" | ", errorMessages));
            }
        }
        return errorMessageBuilder.toString();
    }

    private void checkJobFinishedWithSuccess() throws CloudbreakOrchestratorFailedException {
        String jobId = saltJobRunner.getJid().getJobId();
        try {
            Multimap<String, String> missingNodesWithReason = SaltStates.jidInfo(saltConnector, jobId, new HostList(saltJobRunner.getTargetHostnames()),
                    saltJobRunner.stateType());
            if (!missingNodesWithReason.isEmpty()) {
                LOGGER.info("There are missing nodes after the job (jid: {}) completion: {}", jobId, String.join(",", missingNodesWithReason.keySet()));
                saltJobRunner.setJobState(JobState.FAILED);
                saltJobRunner.setNodesWithError(missingNodesWithReason);
                saltJobRunner.setTargetHostnames(missingNodesWithReason.keySet());
            } else {
                LOGGER.info("The job (jid: {}) completed successfully on every node.", jobId);
                saltJobRunner.setJobState(JobState.FINISHED);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Fail while checking the result (jid: {}), this usually occurs due to concurrency", jobId, e);
            saltJobRunner.setJobState(JobState.AMBIGUOUS);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), saltJobRunner.getNodesWithError());
        }
    }

    @Override
    public String toString() {
        return "SaltJobIdTracker{"
                + "saltJobRunner=" + saltJobRunner
                + '}';
    }
}
