package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class SimpleContainerBootstrapRunner implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleContainerBootstrapRunner.class);
    private static final int MAX_RETRY_COUNT = 7;
    private static final int SLEEP_TIME = 10000;

    private final ContainerBootstrap containerBootstrap;
    private final Map<String, String> mdcMap;
    private final ExitCriteria exitCriteria;
    private final ExitCriteriaModel exitCriteriaModel;

    private SimpleContainerBootstrapRunner(ContainerBootstrap containerBootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
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
        boolean exitNeeded = false;
        while (!success && MAX_RETRY_COUNT >= retryCount && !exitNeeded) {
            exitNeeded = isExitNeeded();
            if (!exitNeeded) {
                try {
                    containerBootstrap.call();
                    success = true;
                    LOGGER.info("Container started successfully.");
                } catch (Exception ex) {
                    success = false;
                    actualException = ex;
                    retryCount++;
                    LOGGER.error(String.format("Container failed to start, retrying [%s/%s]: %s", MAX_RETRY_COUNT, retryCount, ex.getMessage()));
                    Thread.sleep(SLEEP_TIME);
                }
            }
        }
        if (exitNeeded) {
            LOGGER.error(exitCriteria.exitMessage());
            throw new CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage());
        }
        if (!success) {
            LOGGER.error(String.format("Container failed to start in %s attempts: %s", MAX_RETRY_COUNT, actualException));
            throw actualException;
        }
        return Boolean.TRUE;
    }

    private boolean isExitNeeded() {
        if (exitCriteriaModel != null && exitCriteria != null) {
            return exitCriteria.isExitNeeded(exitCriteriaModel);
        } else {
            return false;
        }
    }

    public static SimpleContainerBootstrapRunner simpleContainerBootstrapRunner(ContainerBootstrap containerBootstrap, ExitCriteria exitCriteria,
            ExitCriteriaModel exitCriteriaModel, Map<String, String> mdcMap) {
        return new SimpleContainerBootstrapRunner(containerBootstrap, exitCriteria, exitCriteriaModel, mdcMap);
    }
}
