package com.sequenceiq.datalake.service.sdx;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;

@Service
public class CloudbreakFlowService {

    private static final Integer DOUBLE_CHECK_RETRY_COUNT = 3;

    private static final Integer DOUBLE_CHECK_SLEEP_SEC = 1000;

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
                return flowEndpoint.hasFlowRunning(sdxCluster.getClusterName(), sdxCluster.getLastCbFlowChainId()).getHasActiveFlow();
            }
        } catch (Exception e) {
            LOGGER.error("Exception occured during checking if there is a flow for cluster {} in CB: {}", sdxCluster.getClusterName(), e.getMessage());
            return true;
        }
        return false;
    }
}
