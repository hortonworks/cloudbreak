package com.sequenceiq.datalake.service.upgrade;

import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;

@Component
public class SdxUpgradeValidationResultProvider {

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    public boolean isValidationFailed(SdxCluster sdxCluster) {
        FlowLogResponse lastCloudbreakFlowChainId = cloudbreakFlowService.getLastFlowId(sdxCluster);
        if (lastCloudbreakFlowChainId == null) {
            return false;
        }
        List<FlowLogResponse> flowLogsByFlowId = cloudbreakFlowService.getFlowLogsByFlowId(lastCloudbreakFlowChainId.getFlowId());
        return flowLogsByFlowId.stream().anyMatch(containsValidationInitState()) && flowLogsByFlowId.stream().anyMatch(containsFailedState());
    }

    private Predicate<FlowLogResponse> containsFailedState() {
        return flowLog -> StateStatus.FAILED.equals(flowLog.getStateStatus());
    }

    private Predicate<FlowLogResponse> containsValidationInitState() {
        return flowLog -> "CLUSTER_UPGRADE_VALIDATION_INIT_STATE".equals(flowLog.getCurrentState());
    }

}
