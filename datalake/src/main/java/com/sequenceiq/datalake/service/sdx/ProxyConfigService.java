package com.sequenceiq.datalake.service.sdx;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ProxyConfigService {

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public FlowIdentifier modifyProxyConfig(SdxCluster sdxCluster, String previousProxyConfigCrn) {
        FlowIdentifier flowIdentifier = stackService.modifyProxyConfigInternal(sdxCluster.getCrn(), previousProxyConfigCrn);
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        return sdxReactorFlowManager.triggerModifyProxyConfigTracker(sdxCluster);
    }
}