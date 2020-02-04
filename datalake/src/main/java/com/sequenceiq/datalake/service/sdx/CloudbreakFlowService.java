package com.sequenceiq.datalake.service.sdx;

import static java.lang.Thread.sleep;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

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

    private static final Integer DOUBLE_CHECK_RETRY_COUNT = 3;

    private static final Integer DOUBLE_CHECK_SLEEP_SEC = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFlowService.class);

    @Inject
    private FlowEndpoint flowEndpoint;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public void setCloudbreakFlowChainId(SdxCluster sdxCluster) {
        FlowLogResponse lastFlowByResourceName = flowEndpoint.getLastFlowByResourceName(sdxCluster.getClusterName());
        sdxCluster.setLastCbFlowChainId(lastFlowByResourceName.getFlowChainId());
        sdxClusterRepository.save(sdxCluster);
    }

    public boolean hasActiveFlow(SdxCluster sdxCluster) {
        try {
            String actualCbFlowChainId = sdxCluster.getLastCbFlowChainId();
            if (actualCbFlowChainId != null) {
                boolean hasPendingFlowSteps = getFlowLogsByNameAndFlowChainId(sdxCluster.getClusterName(), actualCbFlowChainId)
                        .stream().anyMatch(pendingFlowLogPredicate());
                if (!hasPendingFlowSteps) {
                    hasPendingFlowSteps = !doubleCheckIfHasNotActiveFlow(sdxCluster.getClusterName(), actualCbFlowChainId);
                }
                return hasPendingFlowSteps;
            }
        } catch (Exception e) {
            LOGGER.error("Exception occured during getting flow logs from CB: {}", e.getMessage());
        }
        return false;
    }

    private boolean doubleCheckIfHasNotActiveFlow(String resourceName, String flowChainId) throws InterruptedException {
        AtomicInteger retryCount = new AtomicInteger(DOUBLE_CHECK_RETRY_COUNT);
        while (retryCount.decrementAndGet() > 0) {
            sleep(DOUBLE_CHECK_SLEEP_SEC);
            if (getFlowLogsByNameAndFlowChainId(resourceName, flowChainId).stream().anyMatch(pendingFlowLogPredicate())) {
                LOGGER.info("It seems there is still a running flow for SDX {} based on double check of active flow!", resourceName);
                return false;
            }
        }
        LOGGER.info("Double check finished, there is still no running flow for SDX {}!", resourceName);
        return true;
    }

    private List<FlowLogResponse> getFlowLogsByNameAndFlowChainId(String resourceName, String flowChainId) {
        return flowEndpoint.getFlowLogsByResourceNameAndChainId(resourceName, flowChainId);
    }

    private Predicate<FlowLogResponse> pendingFlowLogPredicate() {
        return flowLog -> flowLog.getStateStatus().equals(StateStatus.PENDING) || !flowLog.getFinalized();
    }
}
