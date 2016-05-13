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
    private static final Integer MAX_RETRY_COUNT = 30;
    private static final Integer SLEEP_TIME = 5000;

    private final OrchestratorBootstrap orchestratorBootstrap;
    private final Map<String, String> mdcMap;
    private final ExitCriteria exitCriteria;
    private final ExitCriteriaModel exitCriteriaModel;
    private final Integer maxRetryCount;
    private final Integer sleepTime;

    public OrchestratorBootstrapRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcReplica) {
        this(orchestratorBootstrap, exitCriteria, exitCriteriaModel, mdcReplica, MAX_RETRY_COUNT, SLEEP_TIME);
    }

    public OrchestratorBootstrapRunner(OrchestratorBootstrap orchestratorBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcReplica, Integer maxRetryCount, Integer sleepTime) {
        this.orchestratorBootstrap = orchestratorBootstrap;
        this.mdcMap = mdcReplica;
        this.exitCriteria = exitCriteria;
        this.exitCriteriaModel = exitCriteriaModel;
        this.maxRetryCount = maxRetryCount;
        this.sleepTime = sleepTime;
    }

    @Override
    public Boolean call() throws Exception {
        MDC.setContextMap(mdcMap);
        boolean success = false;
        int retryCount = 0;
        Exception actualException = null;
        String type = orchestratorBootstrap.getClass().getSimpleName().replace("Bootstrap", "");
        while (!success && maxRetryCount >= retryCount) {
            if (isExitNeeded()) {
                LOGGER.error(exitCriteria.exitMessage());
                throw new CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage());
            }
            long startTime = System.currentTimeMillis();
            try {
                LOGGER.info("Calling orchestrator bootstrap: {}, additional info: {}", type, orchestratorBootstrap);
                orchestratorBootstrap.call();
                long elapsedTime = System.currentTimeMillis() - startTime;
                success = true;
                LOGGER.info("Orchestrator component {} successfully started! Elapsed time: {} ms, additional info: {}", type, elapsedTime,
                        orchestratorBootstrap);
            } catch (Exception ex) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                actualException = ex;
                retryCount++;
                LOGGER.error("Orchestrator component {} failed to start, retrying [{}/{}] Elapsed time: {} ms; Reason: {}, additional info: {}", type,
                        maxRetryCount, retryCount, elapsedTime, ex.getMessage(), orchestratorBootstrap);
                Thread.sleep(sleepTime);
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
            LOGGER.debug("exitCriteriaModel: {}, exitCriteria: {}", exitCriteriaModel, exitNeeded);
            exitNeeded = exitCriteria.isExitNeeded(exitCriteriaModel);
        }
        LOGGER.debug("isExitNeeded: {}", exitNeeded);
        return exitNeeded;
    }
}
