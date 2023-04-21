package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId.jobId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorInProgressException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTerminateException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SaltJobFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltEmptyResponseException;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class SaltJobIdTracker implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltJobIdTracker.class);

    private final SaltConnector saltConnector;

    private final SaltJobRunner saltJobRunner;

    private final boolean retryOnFail;

    private final SaltStateService saltStateService;

    public SaltJobIdTracker(SaltStateService saltStateService, SaltConnector saltConnector, SaltJobRunner saltJobRunner) {
        this(saltStateService, saltConnector, saltJobRunner, true);
    }

    public SaltJobIdTracker(SaltStateService saltStateService, SaltConnector saltConnector, SaltJobRunner saltJobRunner, boolean retryOnFail) {
        this.saltConnector = saltConnector;
        this.saltJobRunner = saltJobRunner;
        this.retryOnFail = retryOnFail;
        this.saltStateService = saltStateService;
    }

    @Override
    public Boolean call() throws Exception {
        if (JobState.NOT_STARTED.equals(saltJobRunner.getJobState())) {
            LOGGER.debug("Job has not started in the cluster. Starting for first time.");
            checkIsOtherJobRunning();
            submitJob();
            checkIsFinished(saltJobRunner.getJid().getJobId());
        } else if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.debug("Job: {} is running currently checking the current state.", jobId);
            checkIsFinished(jobId);
        } else if (!retryOnFail && JobState.FAILED == saltJobRunner.getJobState()) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.info("Job: {} failed. Terminate execution on these targets: {}", jobId, saltJobRunner.getTargetHostnames());
            throw new CloudbreakOrchestratorTerminateException(buildErrorMessage(), saltJobRunner.getNodesWithError());
        } else if (JobState.FAILED == saltJobRunner.getJobState() || JobState.AMBIGUOUS == saltJobRunner.getJobState()) {
            String jobId = saltJobRunner.getJid().getJobId();
            LOGGER.debug("Job: {} failed in the previous time. Trigger again with these targets: {}", jobId, saltJobRunner.getTargetHostnames());
            submitJob();
            return call();
        }
        if (JobState.IN_PROGRESS.equals(saltJobRunner.getJobState())) {
            String errorMessage = buildErrorMessage();
            LOGGER.warn(String.format("Job: %s is running currently. Details: %s", saltJobRunner.getJid(), errorMessage));
            throw new CloudbreakOrchestratorInProgressException(errorMessage, saltJobRunner.getNodesWithError());
        }
        if (JobState.FAILED == saltJobRunner.getJobState() || JobState.AMBIGUOUS == saltJobRunner.getJobState()) {
            throw new CloudbreakOrchestratorFailedException(buildErrorMessage(), saltJobRunner.getNodesWithError());
        }
        LOGGER.debug("Job (jid: {}) was finished. Triggering next salt event.", saltJobRunner.getJid().getJobId());
        return true;
    }

    private void checkIsOtherJobRunning() throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorInProgressException {
        RunningJobsResponse runningJobs = saltStateService.getRunningJobs(saltConnector);
        List<String> runningJobIds = mapToRunningJobIds(runningJobs);
        if (!runningJobIds.isEmpty()) {
            LOGGER.warn("There are running job(s) with id: {}. Postpone starting the new job until these are finished.", runningJobIds);
            throw new CloudbreakOrchestratorInProgressException("There are running job(s) with id: " + runningJobIds, saltJobRunner.getNodesWithError());
        }
    }

    private void submitJob() throws SaltJobFailedException {
        saltJobRunner.setJid(jobId(saltJobRunner.submit(saltConnector)));
        saltJobRunner.setJobState(JobState.IN_PROGRESS);
    }

    private List<String> mapToRunningJobIds(RunningJobsResponse runningJobs) {
        return runningJobs.getResult().stream()
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private void checkIsFinished(String jobId) throws CloudbreakOrchestratorFailedException {
        boolean jobRunning = saltStateService.jobIsRunning(saltConnector, jobId);
        if (jobRunning) {
            LOGGER.debug("Job: {} is running currently, waiting for next polling attempt.", jobId);
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
        } else {
            LOGGER.debug("Job finished: {}. Collecting missing nodes", jobId);
            checkJobFinishedWithSuccess();
        }
    }

    private String buildErrorMessage() {
        String jobId = saltJobRunner.getJid().getJobId();
        StringBuilder errorMessageBuilder = new StringBuilder();
        LOGGER.warn(String.format("There are missing nodes from job (jid: %s), target: %s", jobId, saltJobRunner.getTargetHostnames()));
        errorMessageBuilder.append(String.format("Target: %s", saltJobRunner.getTargetHostnames()));
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
            Multimap<String, Map<String, String>> missingNodesWithReason = saltStateService.jidInfo(saltConnector, jobId, saltJobRunner.stateType());
            Multimap<String, String> missingNodesWithReplacedReasons =
                    saltConnector.getSaltErrorResolver().resolveErrorMessages(missingNodesWithReason);
            if (!missingNodesWithReplacedReasons.isEmpty()) {
                LOGGER.debug("There are missing nodes after the job (jid: {}) completion: {}",
                        jobId, String.join(",", missingNodesWithReplacedReasons.keySet()));
                saltJobRunner.setJobState(JobState.FAILED);
                saltJobRunner.setNodesWithError(missingNodesWithReplacedReasons);
                saltJobRunner.setTargetHostnames(missingNodesWithReplacedReasons.keySet());
            } else {
                LOGGER.debug("The job (jid: {}) completed successfully on every node.", jobId);
                saltJobRunner.setJobState(JobState.FINISHED);
            }
        } catch (SaltEmptyResponseException e) {
            LOGGER.debug("Jid info is empty (jid: {}), this usually occurs due to salt not working properly", jobId, e);
            saltJobRunner.setJobState(JobState.IN_PROGRESS);
        } catch (RuntimeException e) {
            LOGGER.debug("Fail while checking the result (jid: {}), this usually occurs due to concurrency", jobId, e);
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

    public SaltJobRunner getSaltJobRunner() {
        return saltJobRunner;
    }
}
