package com.sequenceiq.datalake.service.sdx;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;

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

    public boolean isLastKnownFlowRunning(SdxCluster sdxCluster) {
        try {
            String actualCbFlowChainId = sdxCluster.getLastCbFlowChainId();
            if (actualCbFlowChainId != null) {
                List<FlowLogResponse> flowLogsByResourceNameAndChainId =
                        flowEndpoint.getFlowLogsByResourceNameAndChainId(sdxCluster.getClusterName(), actualCbFlowChainId);
                return flowLogsByResourceNameAndChainId.stream()
                        .anyMatch(flowLog -> flowLog.getStateStatus().equals(StateStatus.PENDING) || !flowLog.getFinalized());
            }
        } catch (Exception e) {
            LOGGER.error("Exception occured during getting flow logs from CB: {}", e.getMessage());
        }
        return false;
    }
}
