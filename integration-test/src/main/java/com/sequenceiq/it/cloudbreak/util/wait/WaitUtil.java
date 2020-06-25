package com.sequenceiq.it.cloudbreak.util.wait;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

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

    public DistroXTestDto waitForDistroxInstanceStatus(DistroXTestDto testDto, TestContext testContext, Map<String, InstanceStatus> hostGroupsAndStates) {
        String distroxName = testDto.getResponse().getName();

        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            CloudbreakClient cloudbreakClient = testContext.getMicroserviceClient(CloudbreakClient.class, testParameter.get(CloudbreakTest.ACCESS_KEY));
            boolean notInDesiredState;
            do {
                List<InstanceGroupV4Response> instanceGroups = cloudbreakClient.getCloudbreakClient().distroXV1Endpoint().getByName(distroxName, Set.of())
                        .getInstanceGroups();
                notInDesiredState = checkInstanceState(hostGroup, desiredState, instanceGroups);
                LOGGER.info("Waiting for instance status {} in Host Group {} at {} DistroX, ellapsed {}ms", desiredState, hostGroup, distroxName,
                        System.currentTimeMillis() - startTime);
                StackV4Response distroxResponse = cloudbreakClient.getCloudbreakClient().distroXV1Endpoint().getByName(distroxName, Set.of());
                if (distroxResponse != null) {
                    String distroxStatus = distroxResponse.getStatus().name();
                    String distroxStatusReson = distroxResponse.getStatusReason() != null ? distroxResponse.getStatusReason()
                            : "DistroX Status Reason is not available";
                    if (containsIgnoreCase(distroxStatus, "FAILED")) {
                        LOGGER.error(" Distrox {} is in {} state, because of: {} ", distroxName, distroxStatus, distroxStatusReson);
                        throw new TestFailException("Distrox " + distroxName + " is in " + distroxStatus + " state, because of: " + distroxStatusReson);
                    } else {
                        sleep(pollingInterval);
                        retryCount++;
                    }
                } else {
                    LOGGER.error(" {} Distrox is not present ", distroxName);
                    throw new TestFailException(distroxName + " Distrox is not present. ");
                }
            } while (notInDesiredState && retryCount < maxRetry);

            if (retryCount < maxRetry) {
                List<InstanceGroupV4Response> instanceGroups = cloudbreakClient.getCloudbreakClient().distroXV1Endpoint().getByName(distroxName, Set.of())
                        .getInstanceGroups();
                if (checkInstanceState(hostGroup, desiredState, instanceGroups)) {
                    LOGGER.error(" {} instance group or its metadata cannot be found may it was deleted or missing ", hostGroup);
                    throw new TestFailException(hostGroup + " instance group or its metadata cannot be found may it was deleted or missing. ");
                } else {
                    Log.log(LOGGER, format(" %s host group instance state is %s at %s DistroX", hostGroup, desiredState, distroxName));
                }
            } else {
                LOGGER.error(" Timeout: Host Group: {} desired instance state: {} is NOT available at {} DistroX during {} retries",
                        hostGroup, desiredState, distroxName, maxRetry);
                throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired instance state: " + desiredState
                        + " is NOT available at " + distroxName + " DistroX during " + maxRetry + " retries");
            }
        });

        return testDto;
    }

    public void waitForSdxInstanceStatus(String sdxName, TestContext testContext, Map<String, InstanceStatus> hostGroupsAndStates, boolean checkClusterStatus) {
        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            SdxClient sdxClient = testContext.getMicroserviceClient(SdxClient.class, testParameter.get(CloudbreakTest.ACCESS_KEY));
            boolean notInDesiredState;
            do {
                List<InstanceGroupV4Response> instanceGroups = sdxClient.getSdxClient().sdxEndpoint().getDetail(sdxName, Set.of())
                        .getStackV4Response()
                        .getInstanceGroups();
                notInDesiredState = checkInstanceState(hostGroup, desiredState, instanceGroups);
                LOGGER.info("Waiting for instance status {} in Host Group {} at {} SDX, ellapsed {}ms", desiredState, hostGroup, sdxName,
                        System.currentTimeMillis() - startTime);
                SdxClusterResponse sdxResponse = sdxClient.getSdxClient().sdxEndpoint().get(sdxName);
                if (sdxResponse != null) {
                    String sdxStatus = sdxResponse.getStatus().name();
                    String sdxStatusReason = sdxResponse.getStatusReason() != null ? sdxResponse.getStatusReason()
                            : "SDX Status Reason is not available";
                    if (checkClusterStatus && containsIgnoreCase(sdxStatus, "FAILED")) {
                        LOGGER.error(" SDX {} is in {} state, because of: {}", sdxName, sdxStatus, sdxStatusReason);
                        throw new TestFailException("SDX " + sdxName + " is in " + sdxStatus + " state, because of: " + sdxStatusReason);
                    } else {
                        sleep(pollingInterval);
                        retryCount++;
                    }
                } else {
                    LOGGER.error(" {} SDX is not present ", sdxName);
                    throw new TestFailException(sdxName + " SDX is not present ");
                }
            } while (notInDesiredState && retryCount < maxRetry);

            if (retryCount < maxRetry) {
                List<InstanceGroupV4Response> instanceGroups = sdxClient.getSdxClient().sdxEndpoint().getDetail(sdxName, Set.of())
                        .getStackV4Response()
                        .getInstanceGroups();
                if (checkInstanceState(hostGroup, desiredState, instanceGroups)) {
                    LOGGER.error(" {} instance group or its metadata cannot be found may it was deleted or missing ", hostGroup);
                    throw new TestFailException(hostGroup + " instance group or its metadata cannot be found may it was deleted or missing. ");
                } else {
                    Log.log(LOGGER, format(" %s host group instance state is %s at %s SDX", hostGroup, desiredState, sdxName));
                }
            } else {
                LOGGER.error(" Timeout: Host Group: {} desired instance state: {} is NOT available at {} during {} retries", hostGroup,
                        desiredState, sdxName, maxRetry);
                throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired instance state: " + desiredState
                        + " is NOT available at " + sdxName + " during " + maxRetry + " retries");
            }
        });
    }

    private boolean checkInstanceState(String hostGroup, InstanceStatus desiredState, List<InstanceGroupV4Response> instanceGroups) {
        Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                .stream()
                .filter(ig -> ig.getName().equals(hostGroup))
                .findFirst();
        if (instanceGroup.isPresent()) {
            Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                    .get().getMetadata().stream().findFirst();
            if (instanceMetaData.isPresent()) {
                InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                LOGGER.info(" Instance group {} with {} group state and with {} instance state is present. Desired state {}.",
                        instanceMetaDataV4Response.getInstanceGroup(),
                        instanceMetaDataV4Response.getState(),
                        instanceMetaDataV4Response.getInstanceStatus(),
                        desiredState
                );
                return !Objects.equals(instanceMetaDataV4Response.getInstanceStatus(), desiredState);
            } else {
                LOGGER.error(" instance metadata is empty, may {} instance group was deleted. ", hostGroup);
                return false;
            }
        } else {
            LOGGER.error(" {} instance group is not present, may this was deleted. ", hostGroup);
            return false;
        }
    }

    private void sleep(long pollingInterval) {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Exception has been occurred during wait: ", e);
        }
    }
}