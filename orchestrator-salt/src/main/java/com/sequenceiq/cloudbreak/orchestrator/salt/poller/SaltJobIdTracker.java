package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId.jobId;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorInProgressException;
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
    public Optional<Collection<String>> call() throws Exception {
        if (JobState.NOT_STARTED.equals(saltJobRunner.getJobState())) {
            LOGGER.info("Job has not started in the cluster. Starting for first time.");
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            return checkIsFinished(saltJobRunner.getJid().getJobId());
        } else if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} is running currently checking the current state.", jobId);
            return checkIsFinished(jobId);
        } else if (!retryOnFail && JobState.FAILED == saltJobRunner.getJobState()) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} failed. Terminate execution on these targets: {}", jobId, saltJobRunner.getTargetHostnames());
            return Optional.of(saltJobRunner.getTargetHostnames());
        } else if (JobState.FAILED == saltJobRunner.getJobState() || JobState.AMBIGUOUS == saltJobRunner.getJobState()) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} failed in the previous time. Trigger again with these targets: {}", jobId, saltJobRunner.getTargetHostnames());
            saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
            return call();
        }
        if (JobState.FAILED == saltJobRunner.getJobState() || JobState.AMBIGUOUS == saltJobRunner.getJobState()) {
            LOGGER.warn(buildErrorMessage());
            return Optional.of(saltJobRunner.getTargetHostnames());
        }
        LOGGER.info("Job (jid: {}) was finished. Triggering next salt event.", saltJobRunner.getJid().getJobId());
        return Optional.empty();
    }

    private Optional<Collection<String>> checkIsFinished(String jobId) throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorInProgressException {
        boolean jobRunning = SaltStates.jobIsRunning(saltConnector, jobId);
        if (jobRunning) {
            LOGGER.info("Job: {} is running currently, waiting for next polling attempt.", jobId);
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
            String jobIsRunningMessage = String.format("Job: %s is running currently. Details: %s", saltJobRunner.getJid(), buildErrorMessage());
            throw new CloudbreakOrchestratorInProgressException(jobIsRunningMessage, saltJobRunner.getNodesWithError());
        } else {
            LOGGER.info("Job finished: {}. Collecting missing nodes", jobId);
            return checkJobFinishedWithSuccess();
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

    private Optional<Collection<String>> checkJobFinishedWithSuccess() throws CloudbreakOrchestratorFailedException {
        String jobId = saltJobRunner.getJid().getJobId();
        try {
            Multimap<String, String> missingNodesWithReason = SaltStates.jidInfo(saltConnector, jobId, new HostList(saltJobRunner.getTargetHostnames()),
                    saltJobRunner.stateType());
            if (!missingNodesWithReason.isEmpty()) {
                LOGGER.info("There are missing nodes after the job (jid: {}) completion: {}", jobId, String.join(",", missingNodesWithReason.keySet()));
                saltJobRunner.setJobState(JobState.FAILED);
                saltJobRunner.setNodesWithError(missingNodesWithReason);
                saltJobRunner.setTargetHostnames(missingNodesWithReason.keySet());
                LOGGER.warn(buildErrorMessage());
                return Optional.of(saltJobRunner.getTargetHostnames());
            } else {
                LOGGER.info("The job (jid: {}) completed successfully on every node.", jobId);
                saltJobRunner.setJobState(JobState.FINISHED);
                return Optional.empty();
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Fail while checking the result (jid: {}), this usually occurs due to concurrency", jobId, e);
            saltJobRunner.setJobState(JobState.AMBIGUOUS);
            LOGGER.warn(buildErrorMessage());
            return Optional.of(saltJobRunner.getTargetHostnames());
        }
    }

    @Override
    public String toString() {
        return "SaltJobIdTracker{"
                + "saltJobRunner=" + saltJobRunner
                + '}';
    }
}
