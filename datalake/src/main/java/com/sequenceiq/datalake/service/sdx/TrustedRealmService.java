package com.sequenceiq.datalake.service.sdx;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateTrustedRealmRequest;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class TrustedRealmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedRealmService.class);

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public FlowIdentifier updateTrustedRealm(SdxCluster sdxCluster, UpdateTrustedRealmRequest request) {
        LOGGER.info("Triggering update trusted realm on datalake '{}' with request: {}", sdxCluster.getClusterName(), request);
        FlowIdentifier flowIdentifier = stackService.triggerUpdateTrustedRealm(sdxCluster.getCrn(), request);
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        return sdxReactorFlowManager.triggerUpdateTrustedRealmTracker(sdxCluster);
    }
}
