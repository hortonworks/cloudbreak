package com.sequenceiq.it.cloudbreak.util.wait;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.REQUESTED;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    private static final com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status FREE_IPA_DELETE_COMPLETED =
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    @Value("${integrationtest.testsuite.maxRetry:1800}")
    private int maxRetry;

    public Pair<Status, String> waitAndCheckStatuses(CloudbreakClient cloudbreakClient, String stackName, Status desiredStatus) {
        Pair<Status, String> ret = null;
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(cloudbreakClient, stackName, desiredStatus);
        }
        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("The stack has failed: ").append(System.lineSeparator());
            StackStatusV4Response statusByNameInWorkspace = cloudbreakClient.getCloudbreakClient().stackV4Endpoint()
                    .getStatusByName(cloudbreakClient.getWorkspaceId(), stackName);
            if (statusByNameInWorkspace != null && statusByNameInWorkspace.getStatus() != null) {
                builder.append("statusReason: ").append(statusByNameInWorkspace.getStatusReason());
            }
            throw new RuntimeException(builder.toString());
        } else if (waitResult == WaitResult.TIMEOUT) {
            throw new RuntimeException("Timeout happened");
        } else if (DELETE_COMPLETED != desiredStatus) {
            StackStatusV4Response statusByNameInWorkspace = cloudbreakClient.getCloudbreakClient()
                    .stackV4Endpoint().getStatusByName(cloudbreakClient.getWorkspaceId(), stackName);
            if (statusByNameInWorkspace != null) {
                Object statusReason = statusByNameInWorkspace.getClusterStatusReason();
                if (statusReason != null) {
                    ret = Pair.of(statusByNameInWorkspace.getStatus(), statusByNameInWorkspace.getStatusReason());
                }
            }
        }
        return ret;
    }

    private WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, String stackName, Status desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Status currentStatus = CREATE_IN_PROGRESS;

        int retryCount = 0;
        while (!checkStatuses(currentStatus, desiredStatus) && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, stackName, currentStatus);

            sleep(pollingInterval);
            StackV4Endpoint stackV4Endpoint = cloudbreakClient.getCloudbreakClient().stackV4Endpoint();
            try {
                var statusResult = Optional.ofNullable(stackV4Endpoint.getStatusByName(cloudbreakClient.getWorkspaceId(), stackName).getClusterStatus());
                Status currStatus = DELETE_COMPLETED;
                if (statusResult.isPresent()) {
                    currStatus = statusResult.get();
                }
                currentStatus = currStatus;
            } catch (NotFoundException notFoundException) {
                LOGGER.info("Stack not found: {}", stackName);
                currentStatus = DELETE_COMPLETED;
                break;
            }
            retryCount++;
        }

        if (currentStatus != null && currentStatus.name().contains("FAILED") && hasNoExpectedFailure(currentStatus, desiredStatus)
                || checkNotExpectedDelete(currentStatus, desiredStatus)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatus, stackName, currentStatus);
        } else if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, stackName, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", stackName, currentStatus);
        }
        return waitResult;
    }

    private WaitResult waitForStatuses(FreeIPAClient freeIPAClient, String environmentCrn,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currentStatus =
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;

        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, environmentCrn, currentStatus);

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

        if (currentStatus.name().contains("FAILED") && FREE_IPA_DELETE_COMPLETED != desiredStatus && FREE_IPA_DELETE_COMPLETED == currentStatus) {
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

    private WaitResult waitForStatuses(EnvironmentClient environmentClient, String name,
            EnvironmentStatus desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        EnvironmentStatus currentStatus = EnvironmentStatus.CREATION_INITIATED;

        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, name, currentStatus);

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

        if (currentStatus.name().contains("FAILED") && EnvironmentStatus.ARCHIVED != desiredStatus && EnvironmentStatus.ARCHIVED == currentStatus) {
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
        SdxClusterStatusResponse currentStatus = REQUESTED;

        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, name, currentStatus);

            sleep(pollingInterval);
            SdxEndpoint sdxEndpoint = sdxClient.getSdxClient().sdxEndpoint();
            try {
                Optional<SdxClusterStatusResponse> statusResult = Optional.ofNullable(sdxEndpoint.get(name).getStatus());
                SdxClusterStatusResponse currStatus = DELETED;
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

        if (currentStatus.name().contains("FAILED") || (DELETED != desiredStatus && DELETED == currentStatus)) {
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

    private boolean checkStatuses(Status currentStatus, Status desiredStatus) {
        return desiredStatus == currentStatus;
    }

    private boolean checkFailedStatuses(Status currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == DELETE_COMPLETED;
    }

    private boolean checkFailedStatuses(SdxClusterStatusResponse currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == DELETED;
    }

    private boolean checkFailedStatuses(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
    }

    private boolean checkFailedStatuses(EnvironmentStatus currentStatus) {
        return currentStatus.isFailed() || currentStatus == EnvironmentStatus.ARCHIVED;
    }

    private boolean hasNoExpectedFailure(Status currentStatuses, Status desiredStatuses) {
        return hasNoExpectedStatus(currentStatuses, desiredStatuses);
    }

    private boolean checkNotExpectedDelete(Status currentStatuses, Status desiredStatuses) {
        return hasNoExpectedStatus(currentStatuses, desiredStatuses);
    }

    private boolean hasNoExpectedStatus(Status currentStatus, Status desiredStatus) {
        return DELETE_COMPLETED != desiredStatus && DELETE_COMPLETED == currentStatus;
    }

    public Map<String, String> waitAndCheckStatuses(FreeIPAClient freeIPAClient, String name,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus, long pollingInterval) {
        Map<String, String> errors = new HashMap<>();
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(freeIPAClient, name, desiredStatus);
        }
        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("The stack has failed: ").append(System.lineSeparator());
            DescribeFreeIpaResponse freeIpaResponse = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint()
                    .describe(name);
            if (freeIpaResponse != null && freeIpaResponse.getStatus() != null) {
                builder.append("statusReason: ").append(freeIpaResponse.getStatusReason());
            }
            throw new RuntimeException(builder.toString());
        } else if (waitResult == WaitResult.TIMEOUT) {
            throw new RuntimeException("Timeout happened");
        } else if (com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED != desiredStatus) {
            DescribeFreeIpaResponse freeIpaResponse = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint()
                    .describe(name);
            if (freeIpaResponse != null) {
                    errors = Map.of("status", freeIpaResponse.getStatus().name());
            }
        }
        return errors;
    }

    public Map<String, String> waitAndCheckStatuses(EnvironmentClient environmentClient, String name,
            EnvironmentStatus desiredStatus, long pollingInterval) {
        Map<String, String> errors = new HashMap<>();
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(environmentClient, name, desiredStatus);
        }
        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("The stack has failed: ").append(System.lineSeparator());
            throw new RuntimeException(builder.toString());
        } else if (waitResult == WaitResult.TIMEOUT) {
            throw new RuntimeException("Timeout happened");
        } else if (EnvironmentStatus.ARCHIVED != desiredStatus) {
            DetailedEnvironmentResponse environmentResponse = environmentClient.getEnvironmentClient().environmentV1Endpoint().getByName(name);
            if (environmentResponse != null) {
                    errors = Map.of("status", environmentResponse.getEnvironmentStatus().name());
            }
        }
        return errors;
    }

    public Map<String, String> waitAndCheckStatuses(SdxClient sdxClient, String name,
            SdxClusterStatusResponse desiredStatus, long pollingInterval) {
        Map<String, String> errors = new HashMap<>();
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(sdxClient, name, desiredStatus);
        }
        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("The stack has failed: ").append(System.lineSeparator());
            throw new RuntimeException(builder.toString());
        } else if (waitResult == WaitResult.TIMEOUT) {
            throw new RuntimeException("Timeout happened");
        } else if (SdxClusterStatusResponse.DELETED != desiredStatus) {
            SdxClusterResponse sdxResponse = sdxClient.getSdxClient().sdxEndpoint().get(name);
        if (sdxResponse != null) {
            errors = Map.of("status", sdxResponse.getStatus().name());
        }
    }
        return errors;
    }

    public SdxTestDto waitForSdxInstanceStatus(SdxTestDto sdxTestDto, SdxClient sdxClient, String hostGroup, String desiredState) {
        int retryCount = 0;
        String sdxName = sdxClient.getSdxClient().sdxEndpoint().get(sdxTestDto.getName()).getName();

        pollingInterval = (pollingInterval < 30000) ? 30000 : pollingInterval;
        maxRetry = (maxRetry < 3000) ? 3000 : maxRetry;

        while (retryCount < maxRetry && !checkSdxInstanceGroupStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
            LOGGER.info("Waiting for status {} in Host Group {} at {} SDX", desiredState, hostGroup, sdxName);
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkSdxInstanceGroupStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
            Log.log(LOGGER, format(" Host Group: %s state is: %s at %s SDX OR it cannot be determine right now", hostGroup, desiredState, sdxName));
        } else {
            LOGGER.error("Timeout: Host Group: {} desired state: {} is NOT available at {} SDX during {} retries", hostGroup, desiredState, sdxName, maxRetry);
            throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired state: " + desiredState
                    + " is NOT available at " + sdxName + " SDX during " + maxRetry + " retries");
        }
        return sdxTestDto;
    }

    private boolean checkSdxInstanceGroupStateIsAvailable(SdxClient sdxClient, String sdxName, String hostGroup, String desiredState) {
        InstanceMetaDataV4Response instanceMetaDataV4Response = sdxClient.getSdxClient().sdxEndpoint().getDetail(sdxName, new HashSet<>())
                .getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroup))
                .findFirst()
                .orElse(null).getMetadata().stream().findFirst().orElse(null);
        if (instanceMetaDataV4Response != null) {
            Log.log(LOGGER, format(" Instance Group: %s with State: %s and with Instance State: %s is available. ",
                    instanceMetaDataV4Response.getInstanceGroup(),
                    instanceMetaDataV4Response.getState(),
                    instanceMetaDataV4Response.getInstanceStatus()
            ));
            return instanceMetaDataV4Response.getState().equalsIgnoreCase(desiredState);
        } else {
            Log.log(LOGGER, format(" Instance Metadata is NOT available for %s Host Group! ", hostGroup));
            return true;
        }
    }

    private void sleep(long pollingInterval) {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Exception has been occurred during wait: ", e);
        }
    }
}
