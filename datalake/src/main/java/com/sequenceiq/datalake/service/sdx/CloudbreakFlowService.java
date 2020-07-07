package com.sequenceiq.datalake.service.sdx;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;

@Service
public class CloudbreakFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowService.class);

    @Inject
    private FlowEndpoint flowEndpoint;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public void getAndSaveLastCloudbreakFlowChainId(SdxCluster sdxCluster) {
        FlowLogResponse lastFlowByResourceName = flowEndpoint.getLastFlowByResourceName(sdxCluster.getClusterName());
        LOGGER.info("Found last flow from Cloudbreak, flowId: {} created: {} nextEvent:{} resourceId: {} stateStatus: {}",
                lastFlowByResourceName.getFlowId(),
                lastFlowByResourceName.getCreated(),
                lastFlowByResourceName.getNextEvent(),
                lastFlowByResourceName.getResourceId(),
                lastFlowByResourceName.getStateStatus());
        sdxCluster.setLastCbFlowChainId(lastFlowByResourceName.getFlowChainId());
        sdxClusterRepository.save(sdxCluster);
    }

    public FlowState getLastKnownFlowState(SdxCluster sdxCluster) {
        try {
            if (sdxCluster.getLastCbFlowChainId() != null) {
                LOGGER.info("Checking cloudbreak {} {}", FlowType.FLOW_CHAIN, sdxCluster.getLastCbFlowChainId());
                Boolean hasActiveFlow = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                        flowEndpoint.hasFlowRunningByChainId(sdxCluster.getLastCbFlowChainId()).getHasActiveFlow());
                logCbFlowChainStatus(sdxCluster, hasActiveFlow);
                return getFlowState(hasActiveFlow);
            } else if (sdxCluster.getLastCbFlowId() != null) {
                LOGGER.info("Checking cloudbreak {} {}", FlowType.FLOW, sdxCluster.getLastCbFlowId());
                Boolean hasActiveFlow = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                        flowEndpoint.hasFlowRunningByFlowId(sdxCluster.getLastCbFlowId()).getHasActiveFlow());
                logCbFlowStatus(sdxCluster, hasActiveFlow);
                return getFlowState(hasActiveFlow);
            }
            return FlowState.UNKNOWN;
        } catch (NotFoundException e) {
            LOGGER.error("Flow chain id or resource {} not found in CB: {}, so there is no active flow!", sdxCluster.getClusterName(), e.getMessage());
            return FlowState.UNKNOWN;
        } catch (Exception e) {
            LOGGER.error("Exception occured during checking if there is a flow for cluster {} in CB: {}", sdxCluster.getClusterName(), e.getMessage());
            return FlowState.UNKNOWN;
        }
    }

    private FlowState getFlowState(Boolean hasActiveFlow) {
        if (hasActiveFlow) {
            return FlowState.RUNNING;
        } else {
            return FlowState.FINISHED;
        }
    }

    public enum FlowState {
        RUNNING, FINISHED, UNKNOWN
    }

    public void saveLastCloudbreakFlowChainId(SdxCluster sdxCluster, FlowIdentifier flowIdentifier) {
        if (flowIdentifier == null) {
            LOGGER.info("Cloudbreak not sent flow identifier for cluster falling back to flow API.");
            trySaveLastCbFlowIdOrFlowChainId(sdxCluster);
        } else {
            LOGGER.info("Received cloudbreak flow identifier {} for cluster {}", flowIdentifier, sdxCluster.getClusterName());
            switch (flowIdentifier.getType()) {
                case FLOW:
                    setFlowIdAndResetFlowChainId(sdxCluster, flowIdentifier.getPollableId());
                    break;
                case FLOW_CHAIN:
                    setFlowChainIdAndResetFlowId(sdxCluster, flowIdentifier.getPollableId());
                    break;
                case NOT_TRIGGERED:
                    resetFlowIdAndFlowChainId(sdxCluster);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported flow type " + flowIdentifier.getType());
            }
            sdxClusterRepository.save(sdxCluster);
        }
    }

    private void trySaveLastCbFlowIdOrFlowChainId(SdxCluster sdxCluster) {
        try {
            FlowLogResponse lastFlowByResourceName = flowEndpoint.getLastFlowByResourceName(sdxCluster.getClusterName());
            LOGGER.info("Found last flow from Cloudbreak, flowId: {} created: {} nextEvent:{} resourceId: {} stateStatus: {}",
                    lastFlowByResourceName.getFlowId(),
                    lastFlowByResourceName.getCreated(),
                    lastFlowByResourceName.getNextEvent(),
                    lastFlowByResourceName.getResourceId(),
                    lastFlowByResourceName.getStateStatus());
            if (StringUtils.isNotBlank(lastFlowByResourceName.getFlowChainId())) {
                setFlowChainIdAndResetFlowId(sdxCluster, lastFlowByResourceName.getFlowChainId());
            } else if (StringUtils.isNotBlank(lastFlowByResourceName.getFlowId())) {
                setFlowIdAndResetFlowChainId(sdxCluster, lastFlowByResourceName.getFlowId());
            } else {
                resetFlowIdAndFlowChainId(sdxCluster);
            }
        } catch (Exception ex) {
            LOGGER.info("Not found flow id for cluster: {}. Reset laast flow id to null.", sdxCluster.getClusterName());
            resetFlowIdAndFlowChainId(sdxCluster);
        }
        sdxClusterRepository.save(sdxCluster);
    }

    private void setFlowIdAndResetFlowChainId(SdxCluster sdxCluster, String flowId) {
        sdxCluster.setLastCbFlowId(flowId);
        sdxCluster.setLastCbFlowChainId(null);
    }

    private void setFlowChainIdAndResetFlowId(SdxCluster sdxCluster, String flowChainId) {
        sdxCluster.setLastCbFlowId(null);
        sdxCluster.setLastCbFlowChainId(flowChainId);
    }

    private void resetFlowIdAndFlowChainId(SdxCluster sdxCluster) {
        sdxCluster.setLastCbFlowId(null);
        sdxCluster.setLastCbFlowChainId(null);
    }

    private void logCbFlowChainStatus(SdxCluster sdxCluster, Boolean hasActiveFlow) {
        logActiveStatus(FlowType.FLOW_CHAIN, hasActiveFlow, sdxCluster::getLastCbFlowChainId);
    }

    private void logCbFlowStatus(SdxCluster sdxCluster, Boolean hasActiveFlow) {
        logActiveStatus(FlowType.FLOW, hasActiveFlow, sdxCluster::getLastCbFlowId);
    }

    private void logActiveStatus(FlowType flowType, Boolean hasActiveFlow, Supplier<String> idSupplier) {
        if (hasActiveFlow == null || !hasActiveFlow) {
            LOGGER.info("Cloudbreak {} {} is NOT ACTIVE", flowType, idSupplier.get());
        } else {
            LOGGER.info("Cloudbreak {} {} is ACTIVE", flowType, idSupplier.get());
        }
    }
}
