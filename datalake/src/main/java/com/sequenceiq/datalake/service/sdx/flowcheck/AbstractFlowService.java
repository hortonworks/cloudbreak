package com.sequenceiq.datalake.service.sdx.flowcheck;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;

public abstract class AbstractFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowService.class);

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private FlowCheckResponseToFlowStateConverter flowCheckResponseToFlowStateConverter;

    protected abstract FlowEndpoint flowEndpoint();

    public FlowLogResponse getLastFlowId(String resourceCrn) {
        FlowLogResponse lastFlowByResourceName = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> flowEndpoint().getLastFlowByResourceCrn(resourceCrn));
        logFlowLogResponse(lastFlowByResourceName);
        return lastFlowByResourceName;
    }

    public FlowLogResponse getLastFlowId(SdxCluster sdxCluster) {
        return getLastFlowId(sdxCluster.getStackCrn());
    }

    public FlowState getLastKnownFlowStateByFlowId(String flowId) {
        LOGGER.info("Checking {} {}", FlowType.FLOW, flowId);
        FlowCheckResponse flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> flowEndpoint().hasFlowRunningByFlowId(flowId)
        );
        logFlowStatus(flowId, flowCheckResponse.getHasActiveFlow());
        return flowCheckResponseToFlowStateConverter.convert(flowCheckResponse);
    }

    protected void logCbFlowChainStatus(String flowChainId, Boolean hasActiveFlow) {
        logActiveStatus(FlowType.FLOW_CHAIN, hasActiveFlow, flowChainId);
    }

    protected void logFlowStatus(String flowId, Boolean hasActiveFlow) {
        logActiveStatus(FlowType.FLOW, hasActiveFlow, flowId);
    }

    protected void logActiveStatus(FlowType flowType, Boolean hasActiveFlow, String id) {
        if (hasActiveFlow == null || !hasActiveFlow) {
            LOGGER.info("{} {} is NOT ACTIVE", flowType, id);
        } else {
            LOGGER.info("{} {} is ACTIVE", flowType, id);
        }
    }

    protected void logFlowLogResponse(FlowLogResponse lastFlowByResourceName) {
        LOGGER.info("Found last flow from {}, flowId: {} created: {} nextEvent:{} resourceId: {} stateStatus: {}",
                getClass().getSimpleName(),
                lastFlowByResourceName.getFlowId(),
                lastFlowByResourceName.getCreated(),
                lastFlowByResourceName.getNextEvent(),
                lastFlowByResourceName.getResourceId(),
                lastFlowByResourceName.getStateStatus());
    }
}
