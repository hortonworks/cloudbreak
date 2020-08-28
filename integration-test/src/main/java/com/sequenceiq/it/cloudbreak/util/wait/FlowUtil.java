package com.sequenceiq.it.cloudbreak.util.wait;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
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

    public long getPollingInterval() {
        return pollingInterval;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public SdxTestDto waitBasedOnLastKnownFlow(SdxTestDto sdxTestDto, SdxClient sdxClient) {
        FlowPublicEndpoint flowEndpoint = sdxClient.getSdxClient().flowPublicEndpoint();
        waitForFlow(flowEndpoint, sdxTestDto.getResponse().getCrn(), sdxTestDto.getLastKnownFlowChainId(), sdxTestDto.getLastKnownFlowId());
        return sdxTestDto;
    }

    public SdxInternalTestDto waitBasedOnLastKnownFlow(SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        FlowPublicEndpoint flowEndpoint = sdxClient.getSdxClient().flowPublicEndpoint();
        waitForFlow(flowEndpoint,
                sdxInternalTestDto.getResponse().getCrn(),
                sdxInternalTestDto.getLastKnownFlowChainId(),
                sdxInternalTestDto.getLastKnownFlowId());
        return sdxInternalTestDto;
    }

    public CloudbreakTestDto waitBasedOnLastKnownFlow(CloudbreakTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        FlowPublicEndpoint flowEndpoint = cloudbreakClient.getCloudbreakClient().flowPublicEndpoint();
        waitForFlow(flowEndpoint, distroXTestDto.getCrn(), distroXTestDto.getLastKnownFlowChainId(), distroXTestDto.getLastKnownFlowId());
        return distroXTestDto;
    }

    public EnvironmentTestDto waitBasedOnLastKnownFlow(EnvironmentTestDto environmentTestDto, EnvironmentClient environmentClient) {
        FlowPublicEndpoint flowEndpoint = environmentClient.getEnvironmentClient().flowPublicEndpoint();
        waitForFlow(flowEndpoint, environmentTestDto.getCrn(), environmentTestDto.getLastKnownFlowChainId(), environmentTestDto.getLastKnownFlowId());
        return environmentTestDto;
    }

    public SdxDiagnosticsTestDto waitBasedOnLastKnownFlow(SdxDiagnosticsTestDto sdxDiagnosticsTestDto, SdxClient sdxClient) {
        FlowPublicEndpoint flowEndpoint = sdxClient.getSdxClient().flowPublicEndpoint();
        waitForFlow(flowEndpoint,
                sdxDiagnosticsTestDto.getRequest().getStackCrn(),
                sdxDiagnosticsTestDto.getLastKnownFlowChainId(),
                sdxDiagnosticsTestDto.getLastKnownFlowId());
        return sdxDiagnosticsTestDto;
    }

    public FreeIpaDiagnosticsTestDto waitBasedOnLastKnownFlow(FreeIpaDiagnosticsTestDto freeIpaDiagnosticsTestDto, FreeIpaClient freeIpaClient) {
        FlowPublicEndpoint flowEndpoint = freeIpaClient.getFreeIpaClient().getFlowPublicEndpoint();
        waitForFlow(flowEndpoint,
                freeIpaDiagnosticsTestDto.getFreeIpaCrn(),
                freeIpaDiagnosticsTestDto.getLastKnownFlowChainId(),
                freeIpaDiagnosticsTestDto.getLastKnownFlowId());
        return freeIpaDiagnosticsTestDto;
    }

    private void waitForFlow(FlowPublicEndpoint flowEndpoint, String crn, String flowChainId, String flowId) {
        boolean flowRunning = true;
        int retryCount = 0;
        while (flowRunning && retryCount < maxRetry) {
            sleep(pollingInterval, crn, flowChainId, flowId);
            try {
                if (StringUtils.isNotBlank(flowChainId)) {
                    LOGGER.info("Waiting for flow chain: '{}' at resource: '{}', retry count: '{}'", flowChainId, crn, retryCount);
                    flowRunning = flowEndpoint.hasFlowRunningByChainId(flowChainId, crn).getHasActiveFlow();
                } else if (StringUtils.isNoneBlank(flowId)) {
                    LOGGER.info("Waiting for flow: '{}' at resource: '{}', retry count: '{}'", flowId, crn, retryCount);
                    flowRunning = flowEndpoint.hasFlowRunningByFlowId(flowId, crn).getHasActiveFlow();
                } else {
                    LOGGER.info("Flow id and flow chain id are empty so flow is not running at resource: '{}'", crn);
                    flowRunning = false;
                }
            } catch (Exception ex) {
                LOGGER.error("Error during polling flow. Crn={}, FlowId={}, FlowChainId={}, Message={}", crn, flowId, flowChainId, ex.getMessage(), ex);
                throw new TestFailException(String.format(" Error during polling flow. Crn=%s, FlowId=%s , FlowChainId=%s, Message=%s ",
                        crn, flowId, flowChainId, ex.getMessage()));
            }
            retryCount++;
        }
    }

    private void sleep(long pollingInterval, String crn, String flowChainId, String flowId) {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.error("Waiting for flowId:flowChainId '{}:{}' has been interrupted at resource {}, because of: {}", flowId, flowChainId, crn,
                    e.getMessage(), e);
            throw new TestFailException(String.format(" Waiting for flowId:flowChainId '%s:%s' has been interrupted at resource %s, because of: %s ",
                    flowId, flowChainId, crn, e.getMessage()));
        }
    }
}