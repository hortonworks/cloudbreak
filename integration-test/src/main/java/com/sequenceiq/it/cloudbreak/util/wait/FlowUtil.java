package com.sequenceiq.it.cloudbreak.util.wait;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.polling.AttemptBasedTimeoutChecker;
import com.sequenceiq.cloudbreak.polling.TimeoutChecker;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;

@Component
public class FlowUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowUtil.class);

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 300 : ${integrationtest.testsuite.maxRetry:2700}}")
    private int maxRetry;

    @Value("${integrationtest.testsuite.maxFailureRetry:5}")
    private int maxFailureRetry;

    public long getPollingInterval() {
        return getPollingDurationOrTheDefault(RunningParameter.emptyRunningParameter()).toMillis();
    }

    public Duration getPollingDurationOrTheDefault(RunningParameter runningParameter) {

        if (runningParameter == null) {
            runningParameter = RunningParameter.emptyRunningParameter();
        }

        Duration pollingInterval = runningParameter.getPollingInterval();

        if (pollingInterval == null) {
            pollingInterval = Duration.of(this.pollingInterval, ChronoUnit.MILLIS);
        }
        LOGGER.info("Polling interval is: '{}'", pollingInterval);
        return pollingInterval;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public <T extends CloudbreakTestDto> T waitBasedOnLastKnownFlow(T testDto, MicroserviceClient msClient, TestContext testContext,
            RunningParameter runningParameter) {
        FlowPublicEndpoint flowEndpoint = msClient.flowPublicEndpoint();
        if (flowEndpoint != null) {
            if (testDto == null) {
                throw new RuntimeException("Cloudbreak key has been provided but no result in resource map!");
            }
            String name = testDto.getName();
            try {
                Log.await(LOGGER, String.format(" Cloudbreak await for flow '%s' for '%s'", testDto, name));
                waitForFlow(flowEndpoint, testDto.getCrn(), testDto.getLastKnownFlowChainId(), testDto.getLastKnownFlowId(), runningParameter);
            } catch (Exception e) {
                if (runningParameter.isLogError()) {
                    LOGGER.error("Cloudbreak await for flow '{}' is failed for: '{}', because of {}", testDto, name, e.getMessage(), e);
                    Log.await(LOGGER, String.format(" Cloudbreak await for flow '%s' is failed for '%s', because of %s",
                            testDto, name, e.getMessage()));
                }
                testContext.getExceptionMap().put(String.format("Cloudbreak await for flow %s", testDto), e);
                if (CloudPlatform.YARN.equals(testDto.getCloudPlatform())) {
                    Class<? extends CloudbreakTestDto> testDtoClass = testDto.getClass();
                    if (Set.of(SdxInternalTestDto.class, DistroXTestDto.class).contains(testDtoClass)) {
                        testDto.setMasterPrivateIp(testContext);
                    } else {
                        LOGGER.warn("YCloud cluster logs have not been generated to '{}' testDTO (appropriate resources: Data Lake and Data Hub)!",
                                testDtoClass);
                    }
                }
            }
        }
        return testDto;
    }

    private void waitForFlow(FlowPublicEndpoint flowEndpoint, String crn, String flowChainId, String flowId, RunningParameter runningParameter) {
        boolean flowRunning = true;
        boolean flowFailed = false;
        String flowType = "";
        String flowCurrentState = "";
        String flowReason = "";

        int retryCount = 0;
        int failureCount = 0;
        long pollingInterval = getPollingDurationOrTheDefault(runningParameter).toMillis();
        TimeoutChecker timeoutChecker = Objects.requireNonNullElse(runningParameter.getTimeoutChecker(), new AttemptBasedTimeoutChecker(maxRetry));
        while (flowRunning && !timeoutChecker.checkTimeout()) {
            sleep(pollingInterval, crn, flowChainId, flowId);
            try {
                if (StringUtils.isNotBlank(flowChainId)) {
                    LOGGER.info("Waiting for flow chain: '{}' at resource: '{}', retry count: '{}'", flowChainId, crn, retryCount);
                    FlowCheckResponse flowCheckResponse = flowEndpoint.hasFlowRunningByChainId(flowChainId, crn);
                    flowRunning = flowCheckResponse.getHasActiveFlow();
                    flowFailed = flowCheckResponse.getLatestFlowFinalizedAndFailed();
                    flowType = flowCheckResponse.getFlowType();
                    flowCurrentState = flowCheckResponse.getCurrentState();
                    flowReason = flowCheckResponse.getReason();
                } else if (StringUtils.isNoneBlank(flowId)) {
                    LOGGER.info("Waiting for flow: '{}' at resource: '{}', retry count: '{}'", flowId, crn, retryCount);
                    FlowCheckResponse flowCheckResponse = flowEndpoint.hasFlowRunningByFlowId(flowId, crn);
                    flowRunning = flowCheckResponse.getHasActiveFlow();
                    flowFailed = flowCheckResponse.getLatestFlowFinalizedAndFailed();
                    flowType = flowCheckResponse.getFlowType();
                    flowCurrentState = flowCheckResponse.getCurrentState();
                    flowReason = flowCheckResponse.getReason();
                } else {
                    LOGGER.info("Flow id and flow chain id are empty so flow is not running at resource: '{}'", crn);
                    flowRunning = false;
                }
            } catch (Exception ex) {
                if (failureCount >= maxFailureRetry) {
                    LOGGER.error("Error during polling flow. Crn={}, FlowId={}, FlowChainId={}, FlowType={}, FlowCurrentState={}, FlowReason={}, Message={}",
                            crn, flowId, flowChainId, flowType, flowCurrentState, flowReason, ex.getMessage(), ex);
                    throw new TestFailException(String.format(" Error during polling flow. Crn=%s, FlowId=%s , FlowChainId=%s, FlowType=%s, " +
                                    "FlowCurrentState=%s, FlowReason=%s, Message=%s ",
                            crn, flowId, flowChainId, flowType, flowCurrentState, flowReason, ex.getMessage()));
                } else {
                    LOGGER.info("Retrying after failure. Failure count {}", ++failureCount);
                }
            }
            retryCount++;
        }
        if (timeoutChecker.checkTimeout()) {
            String errorMessage = String.format("Test timed out, flow did not finish in %s. Crn=%s, FlowId=%s, FlowChainId=%s, FlowType=%s, " +
                            "FlowCurrentState=%s, FlowReason=%s",
                    timeoutChecker, crn, flowId, flowChainId, flowType, flowCurrentState, flowReason);
            LOGGER.error(errorMessage);
            throw new TestFailException(errorMessage);
        }
        if (flowFailed && runningParameter.isWaitForFlowSuccess()) {
            LOGGER.error("Flow has been finalized with failed status. Crn={}, FlowId={}, FlowChainId={}, FlowType={}, FlowCurrentState={}, FlowReason={}",
                    crn, flowId, flowChainId, flowType, flowCurrentState, flowReason);
            throw new TestFailException(String.format(" Flow has been finalized with failed status. Crn=%s, FlowId=%s, FlowChainId=%s, FlowType=%s, " +
                            "FlowCurrentState=%s, FlowReason=%s", crn, flowId, flowChainId, flowType, flowCurrentState, flowReason));
        }
        if (!flowFailed && runningParameter.isWaitForFlowFail()) {
            LOGGER.error("Flow has been finalized with success status. Crn={}, FlowId={}, FlowChainId={}, FlowType={}, FlowCurrentState={}, FlowReason={}",
                    crn, flowId, flowChainId, flowType, flowCurrentState, flowReason);
            throw new TestFailException(
                    String.format(" Flow has been finalized with success status but it was expected to fail. Crn=%s, FlowId=%s, FlowChainId=%s, " +
                            "FlowType=%s, FlowCurrentState=%s, FlowReason=%s", crn, flowId, flowChainId, flowType, flowCurrentState, flowReason));
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
