package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class ContainerBootstrapRunner implements Callable<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerBootstrapRunner.class);
    private static final int MAX_RETRY_COUNT = 30;
    private static final int SLEEP_TIME = 5000;

    private final ContainerBootstrap containerBootstrap;
    private final Map<String, String> mdcMap;
    private final ExitCriteria exitCriteria;
    private final ExitCriteriaModel exitCriteriaModel;

    public ContainerBootstrapRunner(ContainerBootstrap containerBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcReplica) {
        this.containerBootstrap = containerBootstrap;
        this.mdcMap = mdcReplica;
        this.exitCriteria = exitCriteria;
        this.exitCriteriaModel = exitCriteriaModel;
    }

    @Override
    public Boolean call() throws Exception {
        MDC.setContextMap(mdcMap);
        boolean success = false;
        int retryCount = 0;
        Exception actualException = null;
        String type = containerBootstrap.getClass().getSimpleName().replace("Bootstrap", "");
        while (!success && MAX_RETRY_COUNT >= retryCount) {
            if (isExitNeeded()) {
                LOGGER.error(exitCriteria.exitMessage());
                throw new CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage());
            }
            long startTime = System.currentTimeMillis();
            try {
                LOGGER.info("Calling container bootstrap: {}, additional info: {}", type, containerBootstrap);
                containerBootstrap.call();
                long elapsedTime = System.currentTimeMillis() - startTime;
                success = true;
                LOGGER.info("Container {} successfully! Elapsed time: {} ms, additional info: {}", type, elapsedTime, containerBootstrap);
            } catch (Exception ex) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                // Swarm is not extremely stable so we retry aggressively in every case
                actualException = ex;
                retryCount++;
                LOGGER.error("Container {} failed to start, retrying [{}/{}] Elapsed time: {} ms; Reason: {}, additional info: {}", type,
                        MAX_RETRY_COUNT, retryCount, elapsedTime, ex.getMessage(), containerBootstrap);
                Thread.sleep(SLEEP_TIME);
            }
        }

        if (!success) {
            LOGGER.error(String.format("Container failed to start in %s attempts: %s", MAX_RETRY_COUNT, actualException));
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
