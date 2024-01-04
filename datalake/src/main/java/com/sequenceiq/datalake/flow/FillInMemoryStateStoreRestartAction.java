package com.sequenceiq.datalake.flow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FillInMemoryStateStoreRestartAction.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        SdxCluster sdxCluster = sdxService.getById(restartContext.getResourceId());
        sdxStatusService.updateInMemoryStateStore(sdxCluster);
        MDCBuilder.buildMdcContext(sdxCluster);
        MDCBuilder.addFlowId(restartContext.getFlowId());
        LOGGER.debug("MDC context and InMemoryStateStore entry have been restored for flow: '{}', flow chain: '{}', event: '{}'", restartContext.getFlowId(),
                restartContext.getFlowChainId(), restartContext.getEvent());
    }
}
