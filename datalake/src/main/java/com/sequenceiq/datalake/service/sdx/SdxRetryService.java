package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_STACK_CREATION_IN_PROGRESS_STATE;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.service.flowlog.FlowRetryUtil;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@Service
public class SdxRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRetryService.class);

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public void retrySdx(SdxCluster sdxCluster) {
        List<FlowLog> flowLogs = flowLogService.findAllByResourceIdOrderByCreatedDesc(sdxCluster.getId());
        if (FlowRetryUtil.isFlowPending(flowLogs)) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow. Sdx name: {}", sdxCluster.getClusterName());
            throw new BadRequestException("Retry cannot be performed, because there is already an active flow.");
        }
        Optional<FlowLog> failedFlowLog = FlowRetryUtil.getMostRecentFailedLog(flowLogs);
        if (failedFlowLog.isEmpty()) {
            throw new BadRequestException("Retry cannot be performed, because the last action was successful");
        }
        failedFlowLog.map(log -> FlowRetryUtil.getLastSuccessfulStateLog(log.getCurrentState(), flowLogs))
                .ifPresent(lastSuccessfulStateLog -> {
                    if (SDX_STACK_CREATION_IN_PROGRESS_EVENT.name().equals(lastSuccessfulStateLog.getNextEvent())) {
                        LOGGER.info("Last successful state was " + SDX_STACK_CREATION_IN_PROGRESS_STATE + ", so try a retry on stack");
                        try {
                            stackV4Endpoint.retry(0L, sdxCluster.getClusterName());
                        } catch (BadRequestException e) {
                            LOGGER.info("Sdx retry failed on cloudbreak side, but try to restart the flow. Related exception: ", e);
                        }
                    }
                    LOGGER.info("Sdx flow restarted: " + lastSuccessfulStateLog);
                    flow2Handler.restartFlow(lastSuccessfulStateLog);
                });
    }

}
