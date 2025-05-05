package com.sequenceiq.datalake.service.sdx.flowcheck;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;

public abstract class AbstractFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowService.class);

    @Inject
    private FlowCheckResponseToFlowStateConverter flowCheckResponseToFlowStateConverter;

    protected abstract FlowEndpoint flowEndpoint();

    public FlowLogResponse getLastFlowId(String resourceCrn) {
        try {
            FlowLogResponse lastFlow = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> flowEndpoint().getLastFlowByResourceCrn(resourceCrn));
            logFlowLogResponse(lastFlow);
            return lastFlow;
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw e;
        }
    }

    public FlowLogResponse getLastFlowId(SdxCluster sdxCluster) {
        return getLastFlowId(sdxCluster.getStackCrn());
    }

    public FlowState getLastKnownFlowStateByFlowId(String flowId) {
        LOGGER.info("Checking {} {}", FlowType.FLOW, flowId);
        FlowCheckResponse flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
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

    protected void logFlowLogResponse(FlowLogResponse lastFlow) {
        LOGGER.info("Found last flow from {}, flowId: {} created: {} nextEvent:{} resourceId: {} stateStatus: {}", getClass().getSimpleName(),
                lastFlow.getFlowId(), lastFlow.getCreated(), lastFlow.getNextEvent(), lastFlow.getResourceId(), lastFlow.getStateStatus());
    }
}
