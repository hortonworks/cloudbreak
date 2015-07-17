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
    private static final int MAX_RETRY_COUNT = 15;
    private static final int SLEEP_TIME = 10000;

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
        boolean exitNeeded = false;
        String type = containerBootstrap.getClass().getSimpleName().replace("Bootstrap", "");
        while (!success && MAX_RETRY_COUNT >= retryCount && !exitNeeded) {
            if (isExitNeeded()) {
                LOGGER.error(exitCriteria.exitMessage());
                throw new CloudbreakOrchestratorCancelledException(exitCriteria.exitMessage());
            }
            try {
                LOGGER.info("Calling container bootstrap: {}", containerBootstrap.getClass().getSimpleName());
                containerBootstrap.call();
                success = true;
                LOGGER.info("Container {} successfully!", type);
            } catch (Exception ex) {
                // Swarm is not extremely stable so we retry aggressively in every case
                actualException = ex;
                retryCount++;
                LOGGER.error("Container {} failed to start, retrying [{}/{}]: {}", type,
                        MAX_RETRY_COUNT, retryCount, ex.getMessage());
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
