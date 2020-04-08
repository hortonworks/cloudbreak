package com.sequenceiq.it.cloudbreak.util.wait;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    private static final com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status FREE_IPA_DELETE_COMPLETED =
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;

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

    private WaitResult waitForStatuses(FreeIPAClient freeIPAClient, String environmentCrn,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currentStatus =
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, environmentCrn, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingInterval);
            FreeIpaV1Endpoint freeIpaV1Endpoint = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint();
            try {
                Optional<com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status> statusResult =
                        Optional.ofNullable(freeIpaV1Endpoint.describe(environmentCrn).getStatus());
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currStatus = FREE_IPA_DELETE_COMPLETED;
                if (statusResult.isPresent()) {
                    currStatus = statusResult.get();
                }
                currentStatus = currStatus;
            } catch (NotFoundException notFoundException) {
                LOGGER.info("Stack not found: {}", environmentCrn);
                currentStatus = FREE_IPA_DELETE_COMPLETED;
                break;
            }
            retryCount++;
        }

        if (currentStatus.name().contains("FAILED") || (FREE_IPA_DELETE_COMPLETED != desiredStatus && FREE_IPA_DELETE_COMPLETED == currentStatus)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatus, environmentCrn, currentStatus);
        } else if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, environmentCrn, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", environmentCrn, currentStatus);
        }
        return waitResult;
    }

    private WaitResult waitForStatuses(RedbeamsClient redbeamsClient, String crn,
            Status desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Status currentStatus = Status.CREATE_IN_PROGRESS;

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, crn, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingInterval);
            DatabaseServerV4Endpoint databaseServerEndpoint = redbeamsClient.getEndpoints().databaseServerV4Endpoint();
            try {
                Optional<Status> statusResult = Optional.ofNullable(databaseServerEndpoint.getByCrn(crn).getStatus());
                Status currStatus = Status.DELETE_COMPLETED;
                if (statusResult.isPresent()) {
                    currStatus = statusResult.get();
                }
                currentStatus = currStatus;
            } catch (NotFoundException notFoundException) {
                LOGGER.info("Stack not found: {}", crn);
                currentStatus = Status.DELETE_COMPLETED;
                break;
            }
            retryCount++;
        }

        if (currentStatus.name().contains("FAILED") || (Status.UNKNOWN != desiredStatus && Status.UNKNOWN == currentStatus)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatus, crn, currentStatus);
        } else if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, crn, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", crn, currentStatus);
        }
        return waitResult;
    }

    private WaitResult waitForStatuses(EnvironmentClient environmentClient, String name,
            EnvironmentStatus desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        EnvironmentStatus currentStatus = EnvironmentStatus.CREATION_INITIATED;

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, name, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingInterval);
            EnvironmentEndpoint environmentEndpoint = environmentClient.getEnvironmentClient().environmentV1Endpoint();
            try {
                Optional<EnvironmentStatus> statusResult = Optional.ofNullable(environmentEndpoint.getByName(name).getEnvironmentStatus());
                EnvironmentStatus currStatus = EnvironmentStatus.ARCHIVED;
                if (statusResult.isPresent()) {
                    currStatus = statusResult.get();
                }
                currentStatus = currStatus;
            } catch (NotFoundException notFoundException) {
                LOGGER.info("Stack not found: {}", name);
                currentStatus = EnvironmentStatus.ARCHIVED;
                break;
            }
            retryCount++;
        }

        if (currentStatus.name().contains("FAILED") || (EnvironmentStatus.ARCHIVED != desiredStatus && EnvironmentStatus.ARCHIVED == currentStatus)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", name, currentStatus);
        }
        return waitResult;
    }

    private WaitResult waitForStatuses(SdxClient sdxClient, String name,
            SdxClusterStatusResponse desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        SdxClusterStatusResponse currentStatus = SdxClusterStatusResponse.REQUESTED;

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, name, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingInterval);
            SdxEndpoint sdxEndpoint = sdxClient.getSdxClient().sdxEndpoint();
            try {
                Optional<SdxClusterStatusResponse> statusResult = Optional.ofNullable(sdxEndpoint.get(name).getStatus());
                SdxClusterStatusResponse currStatus = SdxClusterStatusResponse.DELETED;
                if (statusResult.isPresent()) {
                    currStatus = statusResult.get();
                }
                currentStatus = currStatus;
            } catch (NotFoundException notFoundException) {
                LOGGER.info("SDX not found: {}", name);
                currentStatus = SdxClusterStatusResponse.DELETED;
                break;
            }
            retryCount++;
        }

        if (currentStatus.name().contains("FAILED") || (SdxClusterStatusResponse.DELETED != desiredStatus
                && SdxClusterStatusResponse.DELETED == currentStatus)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", name, currentStatus);
        }
        return waitResult;
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
            retryCount++;
        }
        if (flowRunning) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private boolean checkFailedStatuses(Status currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == Status.DELETE_COMPLETED;
    }

    private boolean checkFailedStatuses(SdxClusterStatusResponse currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == SdxClusterStatusResponse.DELETED;
    }

    private boolean checkFailedStatuses(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
    }

    private boolean checkFailedStatuses(EnvironmentStatus currentStatus) {
        return currentStatus.isFailed() || currentStatus == EnvironmentStatus.ARCHIVED;
    }

    public Map<String, String> waitAndCheckStatuses(RedbeamsClient redbeamsClient, String crn,
            Status desiredStatus, long pollingInterval) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(redbeamsClient, crn, desiredStatus);
        }
        return getDatabaseServerErrors(redbeamsClient, desiredStatus, waitResult, crn);
    }

    public Map<String, String> waitAndCheckStatuses(FreeIPAClient freeIPAClient, String name,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus, long pollingInterval) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(freeIPAClient, name, desiredStatus);
        }
        return getFreeIPAErrors(freeIPAClient, desiredStatus, waitResult, name);
    }

    public Map<String, String> waitAndCheckStatuses(EnvironmentClient environmentClient, String name,
            EnvironmentStatus desiredStatus, long pollingInterval) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(environmentClient, name, desiredStatus);
        }
        return getEnvironmentErrors(environmentClient, desiredStatus, waitResult, name);
    }

    public Map<String, String> waitAndCheckStatuses(SdxClient sdxClient, String name,
            SdxClusterStatusResponse desiredStatus, long pollingInterval) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(sdxClient, name, desiredStatus);
        }
        return getSdxErrors(sdxClient, desiredStatus, waitResult, name);
    }

    public DistroXTestDto waitForDistroxInstanceStatus(DistroXTestDto testDto, TestContext testContext, Map<String, InstanceStatus> hostGroupsAndStates) {
        String distroxName = testDto.getResponse().getName();

        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            CloudbreakClient cloudbreakClient = testContext.getMicroserviceClient(CloudbreakClient.class, testParameter.get(CloudbreakTest.ACCESS_KEY));
            List<InstanceGroupV4Response> instanceGroups = cloudbreakClient.getCloudbreakClient().distroXV1Endpoint().getByName(distroxName, Set.of())
                    .getInstanceGroups();
            while (retryCount < maxRetry && checkInstanceState(hostGroup, desiredState, instanceGroups)) {
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
            }

            if (retryCount < maxRetry) {
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

    public void waitForSdxInstanceStatus(String sdxName, TestContext testContext, Map<String, InstanceStatus> hostGroupsAndStates) {
        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            SdxClient sdxClient = testContext.getMicroserviceClient(SdxClient.class, testParameter.get(CloudbreakTest.ACCESS_KEY));
            List<InstanceGroupV4Response> instanceGroups = sdxClient.getSdxClient().sdxEndpoint().getDetail(sdxName, Set.of())
                    .getStackV4Response()
                    .getInstanceGroups();
            while (retryCount < maxRetry && checkInstanceState(hostGroup, desiredState, instanceGroups)) {
                LOGGER.info("Waiting for instance status {} in Host Group {} at {} SDX, ellapsed {}ms", desiredState, hostGroup, sdxName,
                        System.currentTimeMillis() - startTime);
                SdxClusterResponse sdxResponse = sdxClient.getSdxClient().sdxEndpoint().get(sdxName);
                if (sdxResponse != null) {
                    String sdxStatus = sdxResponse.getStatus().name();
                    String sdxStatusReason = sdxResponse.getStatusReason() != null ? sdxResponse.getStatusReason()
                            : "SDX Status Reason is not available";
                    if (containsIgnoreCase(sdxStatus, "FAILED")) {
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
            }

            if (retryCount < maxRetry) {
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

    private Map<String, String> getDatabaseServerErrors(RedbeamsClient redbeamsClient, Status desiredStatus, WaitResult waitResult, String crn) {
        Map<String, String> errors = new HashMap<>();
        DatabaseServerV4Response databaseServerResponse;

        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("RedBeams has failed: ").append(System.lineSeparator());
            databaseServerResponse = getDatabaseServerResponse(redbeamsClient, crn);
            for (Map.Entry<String, String> error : getDatabaseServerStatusDetails(databaseServerResponse).entrySet()) {
                builder.append("status: ").append(error.getKey()).append(" - ").append("statusReason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getDatabaseServerStatusDetails(databaseServerResponse);
        } else if (waitResult == WaitResult.TIMEOUT) {
            StringBuilder builder = new StringBuilder("Timeout happened while waiting for: ")
                    .append(desiredStatus.name()).append(" status.").append(System.lineSeparator());
            databaseServerResponse = getDatabaseServerResponse(redbeamsClient, crn);
            for (Map.Entry<String, String> error : getDatabaseServerStatusDetails(databaseServerResponse).entrySet()) {
                builder.append("The current status: ").append(error.getKey()).append(" with reason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getDatabaseServerStatusDetails(databaseServerResponse);
        } else if (Status.DELETE_COMPLETED != desiredStatus) {
            databaseServerResponse = getDatabaseServerResponse(redbeamsClient, crn);
            if (databaseServerResponse != null) {
                errors = getDatabaseServerStatusDetails(databaseServerResponse);
            }
        }
        return errors;
    }

    private Map<String, String> getDatabaseServerStatusDetails(DatabaseServerV4Response databaseServerResponse) {
        return Map.of("status", databaseServerResponse.getStatus().name(), "reason",
                databaseServerResponse.getStatusReason() != null ? databaseServerResponse.getStatusReason() : "RedBeams Status Reason is not available");
    }

    private DatabaseServerV4Response getDatabaseServerResponse(RedbeamsClient redbeamsClient, String stackCRN) {
        return redbeamsClient.getEndpoints().databaseServerV4Endpoint().getByCrn(stackCRN);
    }

    private Map<String, String> getFreeIPAErrors(FreeIPAClient freeIPAClient, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus,
            WaitResult waitResult, String name) {
        Map<String, String> errors = new HashMap<>();
        DescribeFreeIpaResponse freeIpaResponse;

        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("FreeIPA has failed: ").append(System.lineSeparator());
            freeIpaResponse = getFreeIPAResponse(freeIPAClient, name);
            for (Map.Entry<String, String> error : getFreeIPAStatusDetails(freeIpaResponse).entrySet()) {
                builder.append("status: ").append(error.getKey()).append(" - ").append("statusReason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getFreeIPAStatusDetails(freeIpaResponse);
        } else if (waitResult == WaitResult.TIMEOUT) {
            StringBuilder builder = new StringBuilder("Timeout happened while waiting for: ")
                    .append(desiredStatus.name()).append(" status.").append(System.lineSeparator());
            freeIpaResponse = getFreeIPAResponse(freeIPAClient, name);
            for (Map.Entry<String, String> error : getFreeIPAStatusDetails(freeIpaResponse).entrySet()) {
                builder.append("The current status: ").append(error.getKey()).append(" with reason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getFreeIPAStatusDetails(freeIpaResponse);
        } else if (com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED != desiredStatus) {
            freeIpaResponse = getFreeIPAResponse(freeIPAClient, name);
            if (freeIpaResponse != null) {
                errors = getFreeIPAStatusDetails(freeIpaResponse);
            }
        }
        return errors;
    }

    private Map<String, String> getFreeIPAStatusDetails(DescribeFreeIpaResponse freeIpaResponse) {
        return Map.of("status", freeIpaResponse.getStatus().name(), "reason",
                freeIpaResponse.getStatusReason() != null ? freeIpaResponse.getStatusReason() : "FreeIPA Status Reason is not available");
    }

    private DescribeFreeIpaResponse getFreeIPAResponse(FreeIPAClient freeIPAClient, String freeIPAName) {
        return freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(freeIPAName);
    }

    private Map<String, String> getEnvironmentErrors(EnvironmentClient environmentClient, EnvironmentStatus desiredStatus, WaitResult waitResult, String name) {
        Map<String, String> errors = new HashMap<>();
        DetailedEnvironmentResponse environmentResponse;

        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("Environment has failed: ").append(System.lineSeparator());
            environmentResponse = getEnvironmentResponse(environmentClient, name);
            for (Map.Entry<String, String> error : getEnvironmentStatusDetails(environmentResponse).entrySet()) {
                builder.append("status: ").append(error.getKey()).append(" - ").append("statusReason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getEnvironmentStatusDetails(environmentResponse);
        } else if (waitResult == WaitResult.TIMEOUT) {
            StringBuilder builder = new StringBuilder("Timeout happened while waiting for: ")
                    .append(desiredStatus.name()).append(" status.").append(System.lineSeparator());
            environmentResponse = getEnvironmentResponse(environmentClient, name);
            for (Map.Entry<String, String> error : getEnvironmentStatusDetails(environmentResponse).entrySet()) {
                builder.append("The current status: ").append(error.getKey()).append(" with reason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getEnvironmentStatusDetails(environmentResponse);
        } else if (EnvironmentStatus.ARCHIVED != desiredStatus) {
            environmentResponse = getEnvironmentResponse(environmentClient, name);
            if (environmentResponse != null) {
                errors = getEnvironmentStatusDetails(environmentResponse);
            }
        }
        return errors;
    }

    private Map<String, String> getEnvironmentStatusDetails(DetailedEnvironmentResponse environmentResponse) {
        return Map.of("status", environmentResponse.getEnvironmentStatus().name(), "reason",
                environmentResponse.getStatusReason() != null ? environmentResponse.getStatusReason() : "Environment Status Reason is not available");
    }

    private DetailedEnvironmentResponse getEnvironmentResponse(EnvironmentClient environmentClient, String environmentName) {
        return environmentClient.getEnvironmentClient().environmentV1Endpoint().getByName(environmentName);
    }

    private Map<String, String> getSdxErrors(SdxClient sdxClient, SdxClusterStatusResponse desiredStatus, WaitResult waitResult, String name) {
        Map<String, String> errors = new HashMap<>();
        SdxClusterResponse sdxResponse;

        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("SDX has failed: ").append(System.lineSeparator());
            sdxResponse = getSDXClusterResponse(sdxClient, name);
            for (Map.Entry<String, String> error : getSdxStatusDetails(sdxResponse).entrySet()) {
                builder.append("status: ").append(error.getKey()).append(" - ").append("statusReason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getSdxStatusDetails(sdxResponse);
        } else if (waitResult == WaitResult.TIMEOUT) {
            StringBuilder builder = new StringBuilder("Timeout happened while waiting for: ")
                    .append(desiredStatus.name()).append(" status.").append(System.lineSeparator());
            sdxResponse = getSDXClusterResponse(sdxClient, name);
            for (Map.Entry<String, String> error : getSdxStatusDetails(sdxResponse).entrySet()) {
                builder.append("The current status: ").append(error.getKey()).append(" with reason: ").append(error.getValue());
            }
            LOGGER.error(builder.toString());
            errors = getSdxStatusDetails(sdxResponse);
        } else if (SdxClusterStatusResponse.DELETED != desiredStatus) {
            sdxResponse = getSDXClusterResponse(sdxClient, name);
            if (sdxResponse != null) {
                errors = getSdxStatusDetails(sdxResponse);
            }
        }
        return errors;
    }

    private Map<String, String> getSdxStatusDetails(SdxClusterResponse sdxResponse) {
        return Map.of("status", sdxResponse.getStatus().name(), "reason",
                sdxResponse.getStatusReason() != null ? sdxResponse.getStatusReason() : "SDX Status Reason is not available");
    }

    private SdxClusterResponse getSDXClusterResponse(SdxClient sdxClient, String sdxName) {
        return sdxClient.getSdxClient().sdxEndpoint().get(sdxName);
    }
}