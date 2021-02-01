package com.sequenceiq.datalake.flow;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Payload datalakePayload = (Payload) payload;
        SdxCluster sdxCluster = sdxService.getById(datalakePayload.getResourceId());
        sdxStatusService.updateInMemoryStateStore(sdxCluster);
        MDCBuilder.buildMdcContext(sdxCluster);
        super.restart(flowParameters, flowChainId, event, payload);
    }
}
