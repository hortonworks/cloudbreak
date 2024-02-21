package com.sequenceiq.externalizedcompute.flow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterStatusService;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FillInMemoryStateStoreRestartAction.class);

    @Inject
    private ExternalizedComputeClusterStatusService statusService;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        ExternalizedComputeCluster externalizedComputeCluster =
                externalizedComputeClusterService.getExternalizedComputeCluster(restartContext.getResourceId());
        statusService.updateInMemoryStateStore(externalizedComputeCluster);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        MDCBuilder.addFlowId(restartContext.getFlowId());
        LOGGER.debug("MDC context and InMemoryStateStore entry have been restored for flow: '{}', flow chain: '{}', event: '{}'", restartContext.getFlowId(),
                restartContext.getFlowChainId(), restartContext.getEvent());
    }
}