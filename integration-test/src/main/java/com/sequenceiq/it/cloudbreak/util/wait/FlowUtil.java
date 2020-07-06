package com.sequenceiq.it.cloudbreak.util.wait;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.WaitResult;

@Component
public class FlowUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowUtil.class);

    @Inject
    private TestParameter testParameter;

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 300 : ${integrationtest.testsuite.maxRetry:2700}}")
    private int maxRetry;

    public long getPollingInterval() {
        return pollingInterval;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public WaitResult waitBasedOnLastKnownFlow(SdxTestDto sdxTestDto, SdxClient sdxClient) {
        FlowEndpoint flowEndpoint = sdxClient.getSdxClient().flowEndpoint();
        return isFlowRunning(flowEndpoint, sdxTestDto.getLastKnownFlowChainId(), sdxTestDto.getLastKnownFlowId());
    }

    public WaitResult waitBasedOnLastKnownFlow(SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        FlowEndpoint flowEndpoint = sdxClient.getSdxClient().flowEndpoint();
        return isFlowRunning(flowEndpoint, sdxInternalTestDto.getLastKnownFlowChainId(), sdxInternalTestDto.getLastKnownFlowId());
    }

    public WaitResult waitBasedOnLastKnownFlow(CloudbreakTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        FlowEndpoint flowEndpoint = cloudbreakClient.getCloudbreakClient().flowEndpoint();
        return isFlowRunning(flowEndpoint, distroXTestDto.getLastKnownFlowChainId(), distroXTestDto.getLastKnownFlowId());
    }

    private WaitResult isFlowRunning(FlowEndpoint flowEndpoint, String flowChainId, String flowId) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        boolean flowRunning = true;
        int retryCount = 0;
        while (flowRunning && retryCount < maxRetry) {
            sleep(pollingInterval);
            try {
                if (StringUtils.isNotBlank(flowChainId)) {
                    LOGGER.info("Waiting for flow chain {}, retry count {}", flowChainId, retryCount);
                    flowRunning = flowEndpoint.hasFlowRunningByChainId(flowChainId).getHasActiveFlow();
                } else if (StringUtils.isNoneBlank(flowId)) {
                    LOGGER.info("Waiting for flow {}, retry count {}", flowId, retryCount);
                    flowRunning = flowEndpoint.hasFlowRunningByFlowId(flowId).getHasActiveFlow();
                } else {
                    LOGGER.info("Flow id and flow chain id are empty so flow is not running");
                    flowRunning = false;
                }
            } catch (Exception ex) {
                LOGGER.warn("Error during polling flow. FlowId=" + flowId + ", FlowChainId=" + flowChainId + ", Message=" + ex.getMessage());
                return waitResult;
            }
            retryCount++;
        }
        if (flowRunning) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private void sleep(long pollingInterval) {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Exception has been occurred during wait: ", e);
        }
    }
}