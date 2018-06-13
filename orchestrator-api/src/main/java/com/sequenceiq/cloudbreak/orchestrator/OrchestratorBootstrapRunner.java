package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorInProgressException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTerminateException;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class OrchestratorBootstrapRunner implements Callable<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorBootstrapRunner.class);

    private static final int MAX_RETRY_COUNT = 30;

    private static final int SLEEP_TIME = 5000;

    private static final int MS_IN_SEC = 1000;

    private static final int SEC_IN_MIN = 60;

    private static final int MAX_RETRY_ON_ERROR = 20;

    private final OrchestratorBootstrap orchestratorBootstrap;

    private final Map<String, String> mdcMap;

    private final ExitCriteria exitCriteria;

    private final ExitCriteriaModel exitCriteriaModel;

    private final int maxRetryCount;

    private final int sleepTime;

    private final int maxRetryOnError;

    public OrchestratorBootstrapRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria,
            ExitCriteriaModel exitCriteriaModel, Map<String, String> mdcReplica) {
        this(orchestratorBootstrap, exitCriteria, exitCriteriaModel, mdcReplica, MAX_RETRY_COUNT, SLEEP_TIME, MAX_RETRY_ON_ERROR);
    }

    public OrchestratorBootstrapRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria,
            ExitCriteriaModel exitCriteriaModel, Map<String, String> mdcReplica,
            int maxRetryCount, int sleepTime, int maxRetryOnError) {
        this.orchestratorBootstrap = orchestratorBootstrap;
        mdcMap = mdcReplica;
        this.exitCriteria = exitCriteria;
        this.exitCriteriaModel = exitCriteriaModel;
        this.maxRetryCount = maxRetryCount;
        this.sleepTime = sleepTime;
        this.maxRetryOnError = maxRetryOnError;
    }

    @Override
    public Boolean call() throws Exception {
        if (mdcMap != null) {
            MDC.setContextMap(mdcMap);
        }

        return doCall();
    }

    private Boolean doCall() throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        Boolean success = null;
        int retryCount = 1;
        int errorCount = 1;
        Exception actualException = null;
        String type = orchestratorBootstrap.getClass().getSimpleName().replace("Bootstrap", "");
        long initialStartTime = System.currentTimeMillis();
        while (success == null && belowAttemptThreshold(retryCount, errorCount)) {
            if (isExitNeeded()) {
                LOGGER.info(exitCriteria.exitMessage());
                throw new CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage());
            }
            long startTime = System.currentTimeMillis();
            try {
                LOGGER.info("Calling orchestrator bootstrap: {}, additional info: {}", type, orchestratorBootstrap);
                orchestratorBootstrap.call();
                success = Boolean.TRUE;
                String elapsedTimeLog = createElapseTimeLog(initialStartTime, startTime);
                LOGGER.info("Orchestrator component {} successfully started! {}, "
                        + "additional info: {}", type, elapsedTimeLog, orchestratorBootstrap);
            } catch (CloudbreakOrchestratorTerminateException te) {
                actualException = te;
                success = Boolean.FALSE;
                String elapsedTimeLog = createElapseTimeLog(initialStartTime, startTime);
                LOGGER.info("Failed to execute orchestrator component {}! {}, "
                        + "additional info: {}", type, elapsedTimeLog, orchestratorBootstrap);
            } catch (CloudbreakOrchestratorInProgressException ex) {
                actualException = ex;
                String elapsedTimeLog = createElapseTimeLog(initialStartTime, startTime);
                LOGGER.warn("Orchestrator component {} start in progress, retrying [{}/{}] {}, Reason: {}, additional info: {}",
                        type, retryCount, maxRetryCount, elapsedTimeLog, actualException, orchestratorBootstrap);
                retryCount++;
                if (retryCount <= maxRetryCount) {
                    trySleeping();
                } else {
                    success = Boolean.FALSE;
                }
            } catch (Exception ex) {
                actualException = ex;
                String elapsedTimeLog = createElapseTimeLog(initialStartTime, startTime);
                LOGGER.warn("Orchestrator component {} failed to start, retrying [{}/{}], error count [{}/{}]. {}, Reason: {}, additional info: {}",
                        type, retryCount, maxRetryCount, errorCount, maxRetryOnError, elapsedTimeLog, actualException, orchestratorBootstrap);
                retryCount++;
                errorCount++;
                if (belowAttemptThreshold(retryCount, errorCount)) {
                    trySleeping();
                } else {
                    success = Boolean.FALSE;
                }
            }
        }

        return checkResult(success, retryCount, actualException);
    }

    private String createElapseTimeLog(long initialStartTime, long startTime) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long totalElapsedTime = System.currentTimeMillis() - initialStartTime;
        String elapsedTimeTemplate = "Elapsed time: %d ms, Total elapsed time: %d ms";
        return String.format(elapsedTimeTemplate, elapsedTime, totalElapsedTime);
    }

    private Boolean checkResult(Boolean success, int retryCount, Exception actualException) throws CloudbreakOrchestratorFailedException {
        if (Boolean.TRUE.equals(success)) {
            return success;
        }

        String cause = null;
        if (actualException != null) {
            cause = actualException.getMessage();
        }
        String messageTemplate = success == null
                ? "Timeout: Orchestrator component failed to finish in %f mins, last message: %s"
                : "Failed: Orchestrator component went failed in %f mins, message: %s";
        String errorMessage = String.format(messageTemplate, (double) retryCount * SLEEP_TIME / MS_IN_SEC / SEC_IN_MIN, cause);
        LOGGER.error(errorMessage);
        throw new CloudbreakOrchestratorFailedException(errorMessage);
    }

    private boolean belowAttemptThreshold(int retryCount, int errorCount) {
        return retryCount <= maxRetryCount && errorCount <= maxRetryOnError;
    }

    private void trySleeping() {
        if (!Thread.interrupted()) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
                LOGGER.warn("The thread was interrupted during sleeping. Sleeping halted, continuing execution.", ie);
            }
        } else {
            LOGGER.warn("The thread was interrupted before sleeping. Skipping sleeping and continuing execution.");
        }
    }

    private boolean isExitNeeded() {
        boolean exitNeeded = false;
        if (exitCriteriaModel != null && exitCriteria != null) {
            LOGGER.debug("exitCriteriaModel: {}, exitCriteria: {}", exitCriteriaModel, exitCriteria);
            exitNeeded = exitCriteria.isExitNeeded(exitCriteriaModel);
        }
        LOGGER.debug("isExitNeeded: {}", exitNeeded);
        return exitNeeded;
    }
}
