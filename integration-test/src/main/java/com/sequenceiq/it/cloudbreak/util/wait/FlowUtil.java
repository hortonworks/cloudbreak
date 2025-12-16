package com.sequenceiq.it.cloudbreak.util.wait;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class FlowUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowUtil.class);

    public void waitForLastKnownFlow(CloudbreakTestDto testDto, FlowPublicEndpoint flowPublicEndpoint, TestContext testContext,
            RunningParameter runningParameter) {
        if (testDto == null) {
            throw new RuntimeException("Cloudbreak key has been provided but no result in resource map!");
        }
        String name = testDto.getName();
        try {
            Log.await(LOGGER, String.format(" Cloudbreak await for flow '%s' for '%s'", testDto, name));
            waitForFlow(flowPublicEndpoint, testDto.getCrn(), testDto.getLastKnownFlowChainId(), testDto.getLastKnownFlowId(), runningParameter, testContext);
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Cloudbreak await for flow '{}' is failed for: '{}', because of {}", testDto, name, e.getMessage(), e);
                Log.await(LOGGER, String.format(" Cloudbreak await for flow '%s' is failed for '%s', because of %s",
                        testDto, name, e.getMessage()));
            }
            testContext.getExceptionMap().put(String.format("Cloudbreak await for flow %s", testDto), e);
            collectPrivateIpsOnYarnProvider(testDto, testContext);
        }
    }

    private void waitForFlow(FlowPublicEndpoint flowEndpoint, String crn, String flowChainId, String flowId, RunningParameter runningParameter,
            TestContext testContext) {
        TimeoutChecker timeoutChecker = getTimeoutChecker(runningParameter, testContext);
        FlowCheckResponse flowCheckResponse = waitForFlowCompletion(testContext, flowEndpoint, crn, flowChainId, flowId, runningParameter, timeoutChecker);
        throwOnTimeout(crn, flowChainId, flowId, flowCheckResponse, timeoutChecker);
        throwOnFlowFailure(crn, flowChainId, flowId, runningParameter, flowCheckResponse);
        throwOnFlowFailureExpected(crn, flowChainId, flowId, runningParameter, flowCheckResponse);
    }

    private FlowCheckResponse waitForFlowCompletion(TestContext testContext, FlowPublicEndpoint flowEndpoint, String crn, String flowChainId, String flowId,
            RunningParameter runningParameter, TimeoutChecker timeoutChecker) {
        if (StringUtils.isAllBlank(flowChainId, flowId)) {
            return null;
        }
        long pollingInterval = testContext.getPollingDurationOrTheDefault(runningParameter).toMillis();
        int retryCount = 0;
        int failureCount = 0;
        FlowCheckResponse flowCheckResponse = null;
        while (!timeoutChecker.checkTimeout()) {
            sleep(pollingInterval, crn, flowChainId, flowId);
            try {
                if (StringUtils.isNotBlank(flowChainId)) {
                    LOGGER.info("Waiting for flow chain: '{}' at resource: '{}', retry count: '{}'", flowChainId, crn, retryCount);
                    flowCheckResponse = flowEndpoint.hasFlowRunningByChainId(flowChainId, crn);
                } else {
                    LOGGER.info("Waiting for flow: '{}' at resource: '{}', retry count: '{}'", flowId, crn, retryCount);
                    flowCheckResponse = flowEndpoint.hasFlowRunningByFlowId(flowId, crn);
                }
                if (!flowCheckResponse.getHasActiveFlow()) {
                    return flowCheckResponse;
                }
            } catch (Exception ex) {
                if (failureCount >= testContext.getMaxRetryCount()) {
                    logErrorAndThrowException("Error during polling flow.", crn, flowChainId, flowId, flowCheckResponse, ex);
                } else {
                    LOGGER.info("Retrying after failure. Failure count {}", ++failureCount);
                }
            }
            retryCount++;
        }
        return flowCheckResponse;
    }

    private void throwOnTimeout(String crn, String flowChainId, String flowId, FlowCheckResponse flowCheckResponse, TimeoutChecker timeoutChecker) {
        if (isActive(flowCheckResponse) && timeoutChecker.checkTimeout()) {
            logErrorAndThrowException(String.format("Test timed out, flow did not finish in %s.", timeoutChecker), crn, flowChainId, flowId, flowCheckResponse,
                    new RuntimeException("Test timed out."));
        }
    }

    private void throwOnFlowFailure(String crn, String flowChainId, String flowId, RunningParameter runningParameter, FlowCheckResponse flowCheckResponse) {
        if (isFailed(flowCheckResponse) && runningParameter.isWaitForFlowSuccess()) {
            logErrorAndThrowException("Flow has been finalized with failed status.", crn, flowChainId, flowId, flowCheckResponse,
                    new RuntimeException("Flow failed."));
        }
    }

    private void throwOnFlowFailureExpected(String crn, String flowChainId, String flowId, RunningParameter runningParameter,
            FlowCheckResponse flowCheckResponse) {
        if (!isFailed(flowCheckResponse) && runningParameter.isWaitForFlowFail()) {
            logErrorAndThrowException("Flow has been finalized with success status but it was expected to fail.", crn, flowChainId, flowId, flowCheckResponse,
                    new RuntimeException("Expected flow failure didn't happen."));
        }
    }

    private boolean isActive(FlowCheckResponse flowCheckResponse) {
        return Optional.ofNullable(flowCheckResponse).map(FlowCheckResponse::getHasActiveFlow).orElse(Boolean.FALSE);
    }

    private boolean isFailed(FlowCheckResponse flowCheckResponse) {
        return Optional.ofNullable(flowCheckResponse).map(FlowCheckResponse::getLatestFlowFinalizedAndFailed).orElse(Boolean.FALSE);
    }

    private void logErrorAndThrowException(String message, String crn, String flowChainId, String flowId, FlowCheckResponse flowCheckResponse, Exception ex) {
        String flowType = flowCheckResponse == null ? "N/A" : flowCheckResponse.getFlowType();
        String currentState = flowCheckResponse == null ? "N/A" : flowCheckResponse.getCurrentState();
        String flowReason = flowCheckResponse == null ? "N/A" : flowCheckResponse.getReason();
        LOGGER.error("{} Crn={}, FlowId={}, FlowChainId={}, FlowType={}, FlowCurrentState={}, FlowReason={}, Message={}",
                message, crn, flowId, flowChainId, flowType, currentState, flowReason, ex.getMessage(), ex);
        throw new TestFailException(String.format(" %s Crn=%s, FlowId=%s , FlowChainId=%s, FlowType=%s, FlowCurrentState=%s, FlowReason=%s, Message=%s ",
                message, crn, flowId, flowChainId, flowType, currentState, flowReason, ex.getMessage()));
    }

    private TimeoutChecker getTimeoutChecker(RunningParameter runningParameter, TestContext testContext) {
        return Objects.requireNonNullElse(runningParameter.getTimeoutChecker(),
                new AttemptBasedTimeoutChecker(testContext.getMaxRetry()));
    }

    private void sleep(long pollingInterval, String crn, String flowChainId, String flowId) {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingInterval);
        } catch (InterruptedException ignored) {
            LOGGER.warn("Waiting for flowId:flowChainId '{}:{}' has been interrupted at resource {}, because of: {}", flowId, flowChainId, crn,
                    ignored.getMessage(), ignored);
        }
    }

    private void collectPrivateIpsOnYarnProvider(CloudbreakTestDto testDto, TestContext testContext) {
        if (CloudPlatform.YARN.equals(testDto.getCloudPlatform())) {
            Class<? extends CloudbreakTestDto> testDtoClass = testDto.getClass();
            if (Set.of(SdxInternalTestDto.class, DistroXTestDto.class).contains(testDtoClass)) {
                testDto.setPrivateIpsForLogCollection(testContext);
            } else {
                LOGGER.warn("YCloud cluster logs have not been generated to '{}' testDTO (appropriate resources: Data Lake and Data Hub)!",
                        testDtoClass);
            }
        }
    }
}
