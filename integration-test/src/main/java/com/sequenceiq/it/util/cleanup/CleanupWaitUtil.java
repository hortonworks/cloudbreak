package com.sequenceiq.it.util.cleanup;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxClient;

@Component
public class CleanupWaitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupWaitUtil.class);

    @Value("${integrationtest.testsuite.pollingInterval:30000}")
    private long pollingInterval;

    @Value("${integrationtest.testsuite.maxRetry:3000}")
    private int maxRetry;

    // Values below 1 are treated as 1 (see validateConfiguration and ConsecutiveErrors).
    @Value("${integrationtest.cleanup.maxConsecutiveErrors:10}")
    private int maxConsecutiveErrors;

    @Value("${integrationtest.cleanup.maxWaitMinutes:120}")
    private long maxWaitMinutes;

    // Grace sleep (ms) after a resource is gone, before the next cleanup stage. Negative values are treated as 0 (see validateConfiguration).
    @Value("${integrationtest.cleanup.deleteSleep:30000}")
    private long deleteSleep;

    @PostConstruct
    void validateConfiguration() {
        if (maxConsecutiveErrors < 1) {
            LOG.warn("integrationtest.cleanup.maxConsecutiveErrors={} is below 1, using 1", maxConsecutiveErrors);
            maxConsecutiveErrors = 1;
        }
        if (deleteSleep < 0) {
            LOG.warn("integrationtest.cleanup.deleteSleep={} is negative, using 0", deleteSleep);
            deleteSleep = 0;
        }
    }

    /**
     * Wait till all the distroxes in all the environments are terminated.
     *
     * Returns with:
     * FAILED:      At least one of the distroxes gets in DELETE_FAILED state
     * TIMEOUT:     There is at least one distrox, out of all, that is still available.
     *              Still present when the wait ended by maxRetry, maxWaitMinutes, the run deadline,
     *              or after maxConsecutiveErrors consecutive API failures.
     * SUCCESSFUL:  All the distroxes in all the environments have been terminated successfully.
     *
     * See {@link #poll} for the exact stop conditions.
     *
     * @param cloudbreak   com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @param runDeadline  Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForDistroxesCleanup(CloudbreakClient cloudbreak, EnvironmentClient environment, RunDeadline runDeadline) {
        Map<String, String> environments = environment.environmentV1Endpoint().list(null).getResponses().stream()
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));

        return poll("DISTROXes",
                errors -> checkDistroxesAreAvailable(cloudbreak, environments, errors),
                () -> checkDistroxesDeleteFailedStatus(cloudbreak, environments), runDeadline).result();
    }

    /**
     * Wait till the data hub (distrox) is deleted (terminated).
     *
     * Returns with:
     * FAILED:      The distrox gets in DELETE_FAILED state
     * TIMEOUT:     The distrox is still available.
     *              Still present when the wait ended by maxRetry, maxWaitMinutes, the run deadline,
     *              or after maxConsecutiveErrors consecutive API failures.
     * SUCCESSFUL:  The distrox has been terminated successfully.
     *
     * See {@link #poll} for the exact stop conditions.
     *
     * @param cloudbreakClient  com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param distroxName       Provided distrox name
     * @param runDeadline       Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return                  FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForDistroxCleanup(CloudbreakClient cloudbreakClient, String distroxName, RunDeadline runDeadline) {
        return poll("distrox " + distroxName,
                errors -> checkDistroxIsAvailable(cloudbreakClient, distroxName, errors),
                () -> checkDistroxDeleteFailedStatus(cloudbreakClient, distroxName), runDeadline).result();
    }

    /**
     * Wait till all the sdxes in all the environments are terminated.
     *
     * Returns with:
     * FAILED:      At least one of the sdxes gets in DELETE_FAILED state
     * TIMEOUT:     There is at least one sdx, out of all, that is still available.
     *              Still present when the wait ended by maxRetry, maxWaitMinutes, the run deadline,
     *              or after maxConsecutiveErrors consecutive API failures.
     * SUCCESSFUL:  All the sdxes in all the environments have been terminated successfully.
     *
     * See {@link #poll} for the exact stop conditions.
     *
     * @param sdx          com.sequenceiq.sdx.client.SdxClient
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @param runDeadline  Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForSdxesCleanup(SdxClient sdx, EnvironmentClient environment, RunDeadline runDeadline) {
        Map<String, String> environments = environment.environmentV1Endpoint().list(null).getResponses().stream()
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));

        return poll("Data Lakes",
                errors -> checkSdxesAreAvailable(sdx, environments, errors),
                () -> checkSdxesDeleteFailedStatus(sdx, environments), runDeadline).result();
    }

    /**
     * Wait till the data lake (sdx) is deleted (terminated).
     *
     * Returns with:
     * FAILED:      The sdx gets in DELETE_FAILED state
     * TIMEOUT:     The sdx is still available.
     *              Still present when the wait ended by maxRetry, maxWaitMinutes, the run deadline,
     *              or after maxConsecutiveErrors consecutive API failures.
     * SUCCESSFUL:  The sdx has been terminated successfully.
     *
     * See {@link #poll} for the exact stop conditions.
     *
     * @param sdxClient    com.sequenceiq.sdx.client.SdxClient
     * @param sdxName      Provided sdx name
     * @param runDeadline  Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForSdxCleanup(SdxClient sdxClient, String sdxName, RunDeadline runDeadline) {
        return poll("Data Lake " + sdxName,
                errors -> checkSdxIsAvailable(sdxClient, sdxName, errors),
                () -> checkSdxDeleteFailedStatus(sdxClient, sdxName), runDeadline).result();
    }

    /**
     * Wait till all the environments are terminated.
     *
     * Returns with:
     * FAILED:      At least one of the environments gets in DELETE_FAILED state
     * TIMEOUT:     There is at least one environment, out of all, that is still available.
     *              Still present when the wait ended by maxRetry, maxWaitMinutes, the run deadline,
     *              or after maxConsecutiveErrors consecutive API failures.
     * SUCCESSFUL:  All the environments have been terminated successfully.
     *
     * See {@link #poll} for the exact stop conditions.
     *
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @param runDeadline  Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForEnvironmentsCleanup(EnvironmentClient environment, RunDeadline runDeadline) {
        return poll("environments",
                errors -> checkEnvironmentsAreAvailable(environment, errors),
                () -> checkEnvironmentsDeleteFailedStatus(environment), runDeadline).result();
    }

    /**
     * Wait till the environment is deleted (archived).
     *
     * Returns with:
     * FAILED:      The environment gets in DELETE_FAILED state
     * TIMEOUT:     The environment is still available.
     *              Still present when the wait ended by maxRetry, maxWaitMinutes, the run deadline,
     *              or after maxConsecutiveErrors consecutive API failures.
     * SUCCESSFUL:  The environment has been terminated successfully.
     *
     * See {@link #poll} for the exact stop conditions.
     *
     * @param environmentClient  com.sequenceiq.environment.client.EnvironmentClient
     * @param environmentName    Provided environment name
     * @param runDeadline        Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return                   FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForEnvironmentCleanup(EnvironmentClient environmentClient, String environmentName, RunDeadline runDeadline) {
        return poll("environment " + environmentName,
                errors -> checkEnvironmentIsAvailable(environmentClient, environmentName, errors),
                () -> checkEnvironmentDeleteFailedStatus(environmentClient, environmentName), runDeadline).result();
    }

    /**
     * Polls until the resource(s) are gone, a DELETE_FAILED state is seen, or one of the bounds below ends the wait.
     * <p>
     * Every iteration checks, in this order, and the first one that applies ends the loop with the given {@link StopReason}:
     * <ol>
     * <li>{@code maxRetry} iterations have been done: MAX_RETRY</li>
     * <li>the deadline of this wait has passed: WAIT_DEADLINE if it is the per-wait limit ("integrationtest.cleanup.maxWaitMinutes"
     * from now), RUN_DEADLINE if the run deadline is earlier and so limits the wait. If both are the same instant,
     * WAIT_DEADLINE is reported</li>
     * <li>the resource(s) are gone: GONE</li>
     * <li>"integrationtest.cleanup.maxConsecutiveErrors" consecutive availability checks have failed: ERROR_LIMIT</li>
     * <li>a DELETE_FAILED state is seen: DELETE_FAILED</li>
     * </ol>
     * Otherwise it sleeps "integrationtest.testsuite.pollingInterval" and goes on. If the run deadline has already passed on
     * entry, the loop does not iterate at all and only the final checks below run.
     * <p>
     * A failed availability check (any API error other than 404 where that means "gone") counts as "still present" and
     * increments the consecutive error counter; every successful check resets it. The DELETE_FAILED check never counts, a failing
     * one is treated as "not failed".
     * <p>
     * After the loop ended, the final checks run in this order. The DELETE_FAILED and availability checks are new API calls, they do not
     * reuse the result that ended the loop:
     * <ul>
     * <li>the error counter has reached the limit: TIMEOUT, ERROR_LIMIT</li>
     * <li>DELETE_FAILED is seen: FAILED, DELETE_FAILED</li>
     * <li>the resource(s) are still present: TIMEOUT, carrying the StopReason that ended the loop (MAX_RETRY, WAIT_DEADLINE or
     * RUN_DEADLINE; in the rare case that the loop ended with GONE or DELETE_FAILED but the final checks disagree, that
     * reason is kept)</li>
     * <li>otherwise: sleeps "integrationtest.cleanup.deleteSleep" and returns SUCCESSFUL, GONE. This holds even if the loop was
     * ended by a bound and the resource(s) vanished just before the final check</li>
     * </ul>
     * The returned {@link StopReason} is exact and never inferred from the clock afterwards.
     * <p>
     * Only the run deadline is recorded on the {@link RunDeadline}: {@link RunDeadline#markCutShort()} is called if and only if
     * the result is TIMEOUT with StopReason RUN_DEADLINE, and the end-of-run summary reports "cut short" only then. A TIMEOUT
     * with WAIT_DEADLINE is a deadline hit too, but of the single wait, so it is not marked: the run deadline did not limit the run.
     *
     * @param what          Human-readable label of the waited resource(s), used in log messages
     * @param available     Tells whether the resource(s) still exist; updates the given error counter (reset on success, increment on failure)
     * @param deleteFailed  Tells whether the resource(s) are in DELETE_FAILED state
     * @param runDeadline   Deadline of the whole cleanup run, after which no wait goes on; marked when it cut this wait short
     * @return              The wait result and the exact reason the wait ended
     */
    PollOutcome poll(String what, Predicate<ConsecutiveErrors> available, BooleanSupplier deleteFailed, RunDeadline runDeadline) {
        int retryCount = 0;
        ConsecutiveErrors errors = new ConsecutiveErrors(maxConsecutiveErrors);
        WaitDeadline deadline = waitDeadline(runDeadline);
        StopReason loopEnd;

        while (true) {
            if (retryCount >= maxRetry) {
                loopEnd = StopReason.MAX_RETRY;
            } else if (System.currentTimeMillis() >= deadline.millis()) {
                loopEnd = deadline.reason();
            } else if (!available.test(errors)) {
                loopEnd = StopReason.GONE;
            } else if (errors.limitReached()) {
                loopEnd = StopReason.ERROR_LIMIT;
            } else if (deleteFailed.getAsBoolean()) {
                loopEnd = StopReason.DELETE_FAILED;
            } else {
                sleep(pollingInterval);
                retryCount++;
                continue;
            }
            break;
        }

        if (errors.limitReached()) {
            LOG.error("Giving up waiting for {}: could not verify state after {} consecutive API failures", what, maxConsecutiveErrors);
            return new PollOutcome(WaitResult.TIMEOUT, StopReason.ERROR_LIMIT);
        }

        if (deleteFailed.getAsBoolean()) {
            LOG.info("Failed: {} cannot be terminated", what);
            return new PollOutcome(WaitResult.FAILED, StopReason.DELETE_FAILED);
        } else if (available.test(errors)) {
            LOG.info("Timeout: {} cannot be terminated, wait ended by {} (maxRetry={}, maxWaitMinutes={})", what, loopEnd, maxRetry, maxWaitMinutes);
            if (loopEnd == StopReason.RUN_DEADLINE) {
                runDeadline.markCutShort();
            }
            return new PollOutcome(WaitResult.TIMEOUT, loopEnd);
        } else {
            sleep(deleteSleep);
            LOG.info("Success: {} terminated", what);
            return new PollOutcome(WaitResult.SUCCESSFUL, StopReason.GONE);
        }
    }

    /**
     * The deadline of a single wait: "integrationtest.cleanup.maxWaitMinutes" from now, but never later than the deadline
     * of the whole cleanup run. The returned reason tells which of the two limits applies: RUN_DEADLINE only if the run
     * deadline is strictly earlier, otherwise WAIT_DEADLINE. If the run deadline has already passed, the poll loop ends in its
     * first iteration without polling and only the final checks run.
     */
    private WaitDeadline waitDeadline(RunDeadline runDeadline) {
        long now = System.currentTimeMillis();
        long waitDeadline = now + TimeUnit.MINUTES.toMillis(maxWaitMinutes);
        if (runDeadline.getEpochMillis() < waitDeadline) {
            LOG.info("Cleanup run deadline limits this wait to {} seconds instead of {} minutes",
                    TimeUnit.MILLISECONDS.toSeconds(Math.max(0, runDeadline.getEpochMillis() - now)), maxWaitMinutes);
            return new WaitDeadline(runDeadline.getEpochMillis(), StopReason.RUN_DEADLINE);
        }
        return new WaitDeadline(waitDeadline, StopReason.WAIT_DEADLINE);
    }

    private void sleep(long pollingInterval) {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException ignored) {
            LOG.warn("Waiting for cleanup has been interrupted, because of: {}", ignored.getMessage(), ignored);
        }
    }

    /**
     * Checking the number of available distroxes across all the environments.
     *
     * Returns with:
     * TRUE:   At least one distrox is still available in one of the environment.
     * FALSE:  Distroxes cannot be found in any of the available environent.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on distroxes availability
     */
    private boolean checkDistroxesAreAvailable(CloudbreakClient cloudbreak, Map<String, String> environments, ConsecutiveErrors errors) {
        try {
            boolean available = environments.entrySet().stream().anyMatch(env ->
                    !cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                            .map(StackViewV4Response::getName)
                            .collect(Collectors.toList()).isEmpty()
            );
            errors.reset();
            return available;
        } catch (Exception e) {
            errors.increment();
            // Transient failure — assume still present so the poll keeps going rather than declaring success on a glitch.
            LOG.warn("Transient error during check distroxes are available, assuming still present: {}", e.getMessage(), e);
            return true;
        }
    }

    /**
     * Checking the data hub (distrox) is still available.
     *
     * Returns with:
     * TRUE:   The distrox is still available.
     * FALSE:  The distrox cannot be found.
     *
     * @param cloudbreakClient  com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param distroxName       Provided distrox name
     * @return                  TRUE or FALSE based on distrox availability
     */
    private boolean checkDistroxIsAvailable(CloudbreakClient cloudbreakClient, String distroxName, ConsecutiveErrors errors) {
        try {
            cloudbreakClient.distroXV1Endpoint().getByName(distroxName, Collections.emptySet());
            errors.reset();
            return true;
        } catch (NotFoundException e) {
            LOG.info("{} distrox not found (404) — treating as deleted", distroxName);
            errors.reset();
            return false;
        } catch (Exception e) {
            errors.increment();
            // Transient failure — assume the resource is still there so we keep polling rather than
            // falsely reporting success on a network/auth glitch.
            LOG.warn("Transient error while checking {} distrox availability, assuming still present: {}", distroxName, e.getMessage(), e);
            return true;
        }
    }

    /**
     * Checking the number of available sdxes across all the environments.
     *
     * Returns with:
     * TRUE:   At least one sdx is still available in one of the environment.
     * FALSE:  Sdxes cannot be found in any of the available environent.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on sdxes availability
     */
    private boolean checkSdxesAreAvailable(SdxClient sdx, Map<String, String> environments, ConsecutiveErrors errors) {
        try {
            boolean available = environments.entrySet().stream().anyMatch(env ->
                    !(sdx.sdxEndpoint().list(env.getValue(), false).stream()
                            .map(SdxClusterResponse::getName).count() == 0)
            );
            errors.reset();
            return available;
        } catch (Exception e) {
            errors.increment();
            // Transient failure — assume still present so the poll keeps going rather than declaring success on a glitch.
            LOG.warn("Transient error during check Data Lakes are available, assuming still present: {}", e.getMessage(), e);
            return true;
        }
    }

    /**
     * Checking the data lake (sdx) is still available.
     *
     * Returns with:
     * TRUE:   The sdx is still available.
     * FALSE:  The sdx cannot be found.
     *
     * @param sdxClient  com.sequenceiq.sdx.client.SdxClient
     * @param sdxName    Provided sdx name
     * @return           TRUE or FALSE based on sdx availability
     */
    private boolean checkSdxIsAvailable(SdxClient sdxClient, String sdxName, ConsecutiveErrors errors) {
        try {
            sdxClient.sdxEndpoint().get(sdxName);
            errors.reset();
            return true;
        } catch (NotFoundException e) {
            LOG.info("{} Data Lake not found (404) — treating as deleted", sdxName);
            errors.reset();
            return false;
        } catch (Exception e) {
            errors.increment();
            // Transient failure — keep polling instead of declaring success on a glitch.
            LOG.warn("Transient error while checking {} Data Lake availability, assuming still present: {}", sdxName, e.getMessage(), e);
            return true;
        }
    }

    /**
     * Checking the number of available environments.
     *
     * Returns with:
     * TRUE:   At least one environment is still available.
     * FALSE:  Environments cannot be found.
     *
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             TRUE or FALSE based on environments availability
     */
    private boolean checkEnvironmentsAreAvailable(EnvironmentClient environment, ConsecutiveErrors errors) {
        try {
            boolean available = !(environment.environmentV1Endpoint().list(null).getResponses().stream()
                    .map(EnvironmentBaseResponse::getName).count() == 0);
            errors.reset();
            return available;
        } catch (Exception e) {
            errors.increment();
            // Transient failure — assume still present so the poll keeps going rather than declaring success on a glitch.
            LOG.warn("Transient error during check environments are available, assuming still present: {}", e.getMessage(), e);
            return true;
        }
    }

    /**
     * Checking the environment is still available.
     *
     * Returns with:
     * TRUE:   The environment is still available.
     * FALSE:  The environment cannot be found.
     *
     * @param environmentClient  com.sequenceiq.environment.client.EnvironmentClient
     * @param environmentName    Provided environment name
     * @return                   TRUE or FALSE based on environments availability
     */
    private boolean checkEnvironmentIsAvailable(EnvironmentClient environmentClient, String environmentName, ConsecutiveErrors errors) {
        try {
            boolean available = environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                    .anyMatch(response -> response.getName().equalsIgnoreCase(environmentName));
            errors.reset();
            return available;
        } catch (Exception e) {
            errors.increment();
            // Transient failure — assume still present so we keep polling rather than declaring success on a glitch.
            LOG.warn("Transient error while checking {} environment availability, assuming still present: {}", environmentName, e.getMessage(), e);
            return true;
        }
    }

    /**
     * Checking DELETE_FAILED state of available distroxes across all the environments.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkDistroxesDeleteFailedStatus(CloudbreakClient cloudbreak, Map<String, String> environments) {
        try {
            return environments.entrySet().stream().anyMatch(env ->
                    cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                            .anyMatch(response -> response.getStatus().equals(Status.DELETE_FAILED))
            );
        } catch (Exception e) {
            // Transient failure — cannot confirm DELETE_FAILED so assume not failed and let the "are available"
            // check drive the poll loop instead.
            LOG.warn("Transient error during check distroxes DELETE_FAILED state, assuming not failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the data hub (distrox) is in DELETE_FAILED state.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param cloudbreakClient  com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param distroxName       Provided distrox name
     * @return                  TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkDistroxDeleteFailedStatus(CloudbreakClient cloudbreakClient, String distroxName) {
        try {
            return cloudbreakClient.distroXV1Endpoint().getByName(distroxName, Collections.emptySet()).getStatus().equals(Status.DELETE_FAILED);
        } catch (NotFoundException e) {
            // Resource is gone — definitely not in DELETE_FAILED state.
            return false;
        } catch (Exception e) {
            LOG.warn("Transient error while checking {} distrox's DELETE_FAILED state, assuming not failed: {}", distroxName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking DELETE_FAILED state of available sdxes across all the environments.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkSdxesDeleteFailedStatus(SdxClient sdx, Map<String, String> environments) {
        try {
            return environments.entrySet().stream().anyMatch(env ->
                    sdx.sdxEndpoint().list(env.getValue(), false).stream()
                            .anyMatch(response -> response.getStatus().equals(SdxClusterStatusResponse.DELETE_FAILED))
            );
        } catch (Exception e) {
            // Transient failure — cannot confirm DELETE_FAILED so assume not failed and let the "are available"
            // check drive the poll loop instead.
            LOG.warn("Transient error during check Data Lakes DELETE_FAILED state, assuming not failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the data lake (sdx) is in DELETE_FAILED state.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param sdxClient  com.sequenceiq.sdx.client.SdxClient
     * @param sdxName    Provided sdx name
     * @return           TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkSdxDeleteFailedStatus(SdxClient sdxClient, String sdxName) {
        try {
            return sdxClient.sdxEndpoint().get(sdxName).getStatus().equals(SdxClusterStatusResponse.DELETE_FAILED);
        } catch (NotFoundException e) {
            // Resource is gone — definitely not in DELETE_FAILED state.
            return false;
        } catch (Exception e) {
            LOG.warn("Transient error while checking {} Data Lake's DELETE_FAILED state, assuming not failed: {}", sdxName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking DELETE_FAILED state of available environments.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkEnvironmentsDeleteFailedStatus(EnvironmentClient environment) {
        try {
            return environment.environmentV1Endpoint().list(null).getResponses().stream()
                    .anyMatch(response -> response.getEnvironmentStatus().equals(EnvironmentStatus.DELETE_FAILED));
        } catch (Exception e) {
            // Transient failure — cannot confirm DELETE_FAILED so assume not failed and let the "are available"
            // check drive the poll loop instead.
            LOG.warn("Transient error during check environments DELETE_FAILED state, assuming not failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the environment is in DELETE_FAILED state.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state is not available.
     *
     * @param environmentClient  com.sequenceiq.environment.client.EnvironmentClient
     * @param environmentName    Provided environment name
     * @return                   TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkEnvironmentDeleteFailedStatus(EnvironmentClient environmentClient, String environmentName) {
        try {
            Optional<EnvironmentStatus> environmentStatus = environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                    .filter(response -> response.getName().equalsIgnoreCase(environmentName))
                    .findFirst()
                    .map(EnvironmentBaseResponse::getEnvironmentStatus);
            if (environmentStatus.isEmpty()) {
                // Not in the list — the environment has been deleted; not DELETE_FAILED.
                LOG.info("{} environment is no longer listed — treating as deleted", environmentName);
                return false;
            }
            LOG.info("{} environment actual state is: {}", environmentName, environmentStatus.get());
            return environmentStatus.get().equals(EnvironmentStatus.DELETE_FAILED);
        } catch (Exception e) {
            LOG.warn("Transient error while checking {} environment's DELETE_FAILED state, assuming not failed: {}", environmentName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * The bound or event that ended a {@link #poll}. See the poll Javadoc for the exact rules.
     */
    enum StopReason {
        MAX_RETRY, WAIT_DEADLINE, RUN_DEADLINE, ERROR_LIMIT, DELETE_FAILED, GONE
    }

    record PollOutcome(WaitResult result, StopReason stopReason) {
    }

    private record WaitDeadline(long millis, StopReason reason) {
    }

    static final class ConsecutiveErrors {

        private final int limit;

        private int count;

        /**
         * @param limit  Number of consecutive errors after which the limit is reached; values below 1 are treated as 1,
         *               otherwise the limit would be reached before any error happened
         */
        ConsecutiveErrors(int limit) {
            this.limit = Math.max(1, limit);
        }

        void reset() {
            count = 0;
        }

        void increment() {
            count++;
        }

        boolean limitReached() {
            return count >= limit;
        }
    }
}
