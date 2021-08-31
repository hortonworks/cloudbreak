package com.sequenceiq.it.cloudbreak.util.wait;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class FlowUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowUtil.class);

    @Inject
    private TestParameter testParameter;

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 300 : ${integrationtest.testsuite.maxRetry:2700}}")
    private int maxRetry;

    private boolean flowFailed;

    public long getPollingInterval() {
        return pollingInterval;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public boolean isFlowFailed() {
        return BooleanUtils.toBooleanDefaultIfNull(flowFailed, false);
    }

    public <T extends CloudbreakTestDto> T waitBasedOnLastKnownFlow(T testDto, MicroserviceClient msClient, RunningParameter runningParameter) {
        FlowPublicEndpoint flowEndpoint = msClient.flowPublicEndpoint();
        if (flowEndpoint != null) {
            waitForFlow(flowEndpoint, testDto.getCrn(), testDto.getLastKnownFlowChainId(), testDto.getLastKnownFlowId(), runningParameter);
        }
        return testDto;
    }

    private void waitForFlow(FlowPublicEndpoint flowEndpoint, String crn, String flowChainId, String flowId, RunningParameter runningParameter) {
        boolean flowRunning = true;
        int retryCount = 0;
        while (flowRunning && retryCount < maxRetry) {
            sleep(pollingInterval, crn, flowChainId, flowId);
            try {
                if (StringUtils.isNotBlank(flowChainId)) {
                    LOGGER.info("Waiting for flow chain: '{}' at resource: '{}', retry count: '{}'", flowChainId, crn, retryCount);
                    FlowCheckResponse flowCheckResponse = flowEndpoint.hasFlowRunningByChainId(flowChainId, crn);
                    flowRunning = flowCheckResponse.getHasActiveFlow();
                } else if (StringUtils.isNoneBlank(flowId)) {
                    LOGGER.info("Waiting for flow: '{}' at resource: '{}', retry count: '{}'", flowId, crn, retryCount);
                    FlowCheckResponse flowCheckResponse = flowEndpoint.hasFlowRunningByFlowId(flowId, crn);
                    flowRunning = flowCheckResponse.getHasActiveFlow();
                    flowFailed = flowCheckResponse.getLatestFlowFinalizedAndFailed();
                } else {
                    LOGGER.info("Flow id and flow chain id are empty so flow is not running at resource: '{}'", crn);
                    flowRunning = false;
                }
            } catch (Exception ex) {
                flowFailed = true;
                LOGGER.error("Error during polling flow. Crn={}, FlowId={}, FlowChainId={}, Message={}", crn, flowId, flowChainId, ex.getMessage(), ex);
                throw new TestFailException(String.format(" Error during polling flow. Crn=%s, FlowId=%s , FlowChainId=%s, Message=%s ",
                        crn, flowId, flowChainId, ex.getMessage()));
            }
            retryCount++;
        }
        if (isFlowFailed() && runningParameter.isWaitForFlowSuccess()) {
            LOGGER.error("Flow has been finalized with failed status. Crn={}, FlowId={}, FlowChainId={}", crn, flowId, flowChainId);
            throw new TestFailException(String.format(" Flow has been finalized with failed status. Crn=%s, FlowId=%s , FlowChainId=%s ", crn, flowId,
                    flowChainId));
        }
        if (!isFlowFailed() && runningParameter.isWaitForFlowFail()) {
            LOGGER.error("Flow has been finalized with success status. Crn={}, FlowId={}, FlowChainId={}", crn, flowId, flowChainId);
            throw new TestFailException(
                    String.format(" Flow has been finalized with success status but it was expected to fail. Crn=%s, FlowId=%s , FlowChainId=%s ", crn, flowId,
                    flowChainId));
        }
    }

    private void sleep(long pollingInterval, String crn, String flowChainId, String flowId) {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingInterval);
        } catch (InterruptedException ignored) {
            LOGGER.warn("Waiting for flowId:flowChainId '{}:{}' has been interrupted at resource {}, because of: {}", flowId, flowChainId, crn,
                    ignored.getMessage(), ignored);
        }
    }
}