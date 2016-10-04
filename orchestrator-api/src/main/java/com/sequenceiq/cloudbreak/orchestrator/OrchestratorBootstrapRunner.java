package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class OrchestratorBootstrapRunner implements Callable<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorBootstrapRunner.class);
    private static final int MAX_RETRY_COUNT = 30;
    private static final int SLEEP_TIME = 5000;

    private final OrchestratorBootstrap orchestratorBootstrap;
    private final Map<String, String> mdcMap;
    private final ExitCriteria exitCriteria;
    private final ExitCriteriaModel exitCriteriaModel;
    private final int maxRetryCount;
    private final int sleepTime;

    public OrchestratorBootstrapRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria,
            ExitCriteriaModel exitCriteriaModel, Map<String, String> mdcReplica) {
        this(orchestratorBootstrap, exitCriteria, exitCriteriaModel, mdcReplica, MAX_RETRY_COUNT, SLEEP_TIME);
    }

    public OrchestratorBootstrapRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria,
            ExitCriteriaModel exitCriteriaModel, Map<String, String> mdcReplica,
            int maxRetryCount, int sleepTime) {
        this.orchestratorBootstrap = orchestratorBootstrap;
        this.mdcMap = mdcReplica;
        this.exitCriteria = exitCriteria;
        this.exitCriteriaModel = exitCriteriaModel;
        this.maxRetryCount = maxRetryCount;
        this.sleepTime = sleepTime;
    }

    @Override
    public Boolean call() throws Exception {
        if (mdcMap != null) {
            MDC.setContextMap(mdcMap);
        }
        boolean success = false;
        int retryCount = 1;
        Exception actualException = null;
        String type = orchestratorBootstrap.getClass().getSimpleName().replace("Bootstrap", "");
        long initialStartTime = System.currentTimeMillis();
        while (!success && retryCount <= maxRetryCount) {
            if (isExitNeeded()) {
                LOGGER.error(exitCriteria.exitMessage());
                throw new CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage());
            }
            long startTime = System.currentTimeMillis();
            try {
                LOGGER.info("Calling orchestrator bootstrap: {}, additional info: {}", type, orchestratorBootstrap);
                orchestratorBootstrap.call();
                long elapsedTime = System.currentTimeMillis() - startTime;
                long totalElapsedTime = System.currentTimeMillis() - initialStartTime;
                success = true;
                LOGGER.info("Orchestrator component {} successfully started! Elapsed time: {} ms, Total elapsed time: {} ms, "
                        + "additional info: {}", type, elapsedTime, totalElapsedTime, orchestratorBootstrap);
            } catch (Exception ex) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long totalElapsedTime = System.currentTimeMillis() - initialStartTime;
                actualException = ex;
                LOGGER.warn("Orchestrator component {} failed to start, retrying [{}/{}] Elapsed time: {} ms, "
                                + "Total elapsed time: {} ms, Reason: {}, additional info: {}",
                        type, retryCount, maxRetryCount, elapsedTime, totalElapsedTime, actualException.getMessage(), orchestratorBootstrap);
                retryCount++;
                if (retryCount <= maxRetryCount) {
                    Thread.sleep(sleepTime);
                }
            }
        }

        if (!success) {
            LOGGER.error(String.format("Orchestrator component failed to start in %s attempts: %s", maxRetryCount, actualException));
            throw actualException;
        }
        return Boolean.TRUE;
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
