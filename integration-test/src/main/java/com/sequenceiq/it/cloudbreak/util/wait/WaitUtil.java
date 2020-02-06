package com.sequenceiq.it.cloudbreak.util.wait;


import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.REQUESTED;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
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
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
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

    @Inject
    private PollingConfigProvider pollingConfigProvider;

    private WaitResult waitForStatuses(FreeIPAClient freeIPAClient, String environmentCrn,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currentStatus =
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < pollingConfigProvider.getMaxRetry()) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, environmentCrn, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingConfigProvider.getPollingInterval());
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
        } else if (retryCount == pollingConfigProvider.getMaxRetry()) {
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

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < pollingConfigProvider.getMaxRetry()) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, name, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingConfigProvider.getPollingInterval());
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
        } else if (retryCount == pollingConfigProvider.getMaxRetry()) {
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

        long startTime = System.currentTimeMillis();
        int retryCount = 0;
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < pollingConfigProvider.getMaxRetry()) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {}, ellapsed {}ms ...", desiredStatus, name, currentStatus,
                    System.currentTimeMillis() - startTime);

            sleep(pollingConfigProvider.getPollingInterval());
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
                currentStatus = DELETED;
                break;
            }
            retryCount++;
        }

        if (currentStatus.name().contains("FAILED") || (DELETED != desiredStatus && DELETED == currentStatus)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else if (retryCount == pollingConfigProvider.getMaxRetry()) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", name, currentStatus);
        }
        return waitResult;
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

    public Map<String, String> waitAndCheckStatuses(FreeIPAClient freeIPAClient, String name,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatus) {
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
            EnvironmentStatus desiredStatus) {
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
            SdxClusterStatusResponse desiredStatus) {
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

    public SdxTestDto waitForSdxInstanceStatus(SdxTestDto sdxTestDto, SdxClient sdxClient, String hostGroup, InstanceStatus desiredState) {
        int retryCount = 0;
        String sdxName = sdxClient.getSdxClient().sdxEndpoint().get(sdxTestDto.getName()).getName();

        long startTime = System.currentTimeMillis();
        while (retryCount < pollingConfigProvider.getMaxRetry() && !checkSdxInstanceStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
            LOGGER.info("Waiting for instance status {} in Host Group {} at {} SDX, ellapsed {}ms", desiredState, hostGroup, sdxName,
                    System.currentTimeMillis() - startTime);
            sleep(pollingConfigProvider.getPollingInterval());
            retryCount++;
        }

        if (checkSdxInstanceStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
            Log.log(LOGGER, format(" [%s] host group instance state is [%s] at [%s] SDX.", hostGroup, desiredState, sdxName));
        } else {
            LOGGER.error("Timeout: Host Group: {} desired instance state: {} is NOT available at {} SDX during {} retries", hostGroup, desiredState, sdxName,
                    pollingConfigProvider.getMaxRetry());
            throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired instance state: " + desiredState
                    + " is NOT available at " + sdxName + " SDX during " + pollingConfigProvider.getMaxRetry() + " retries");
        }
        return sdxTestDto;
    }

    public SdxInternalTestDto waitForSdxInstancesStatus(SdxInternalTestDto sdxTestDto, SdxClient sdxClient, Map<String, InstanceStatus> hostGroupsAndStates) {
        String sdxName = sdxClient.getSdxClient().sdxEndpoint().get(sdxTestDto.getName()).getName();

        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            while (retryCount < pollingConfigProvider.getMaxRetry() && !checkSdxInstanceStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
                LOGGER.info("Waiting for instance status {} in Host Group {} at {} SDX, ellapsed {}ms", desiredState, hostGroup, sdxName,
                        System.currentTimeMillis() - startTime);
                sleep(pollingConfigProvider.getPollingInterval());
                retryCount++;
            }

            if (checkSdxInstanceStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
                Log.log(LOGGER, format(" [%s] host group instance state is [%s] at [%s] SDX.", hostGroup, desiredState, sdxName));
            } else {
                LOGGER.error("Timeout: Host Group: {} desired instance state: {} is NOT available at {} SDX during {} retries", hostGroup, desiredState, sdxName,
                        pollingConfigProvider.getMaxRetry());
                throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired instance state: " + desiredState
                        + " is NOT available at " + sdxName + " SDX during " + pollingConfigProvider.getMaxRetry() + " retries");
            }
        });

        return sdxTestDto;
    }

    public SdxTestDto waitForSdxInstancesStatus(SdxTestDto sdxTestDto, SdxClient sdxClient, Map<String, InstanceStatus> hostGroupsAndStates) {
        String sdxName = sdxClient.getSdxClient().sdxEndpoint().get(sdxTestDto.getName()).getName();

        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            while (retryCount < pollingConfigProvider.getMaxRetry() && !checkSdxInstanceStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
                LOGGER.info("Waiting for instance status {} in Host Group {} at {} SDX, ellapsed {}ms", desiredState, hostGroup, sdxName,
                        System.currentTimeMillis() - startTime);
                sleep(pollingConfigProvider.getPollingInterval());
                retryCount++;
            }

            if (checkSdxInstanceStateIsAvailable(sdxClient, sdxName, hostGroup, desiredState)) {
                Log.log(LOGGER, format(" [%s] host group instance state is [%s] at [%s] SDX.", hostGroup, desiredState, sdxName));
            } else {
                LOGGER.error("Timeout: Host Group: {} desired instance state: {} is NOT available at {} SDX during {} retries", hostGroup, desiredState,
                        sdxName, pollingConfigProvider.getMaxRetry());
                throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired instance state: " + desiredState
                        + " is NOT available at " + sdxName + " SDX during " + pollingConfigProvider.getMaxRetry() + " retries");
            }
        });

        return sdxTestDto;
    }

    public DistroXTestDto waitForDistroxInstancesStatus(DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient, Map<String,
            InstanceStatus> hostGroupsAndStates) {
        String distroxName = distroXTestDto.getRequest().getName();

        hostGroupsAndStates.forEach((hostGroup, desiredState) -> {
            int retryCount = 0;
            long startTime = System.currentTimeMillis();
            while (retryCount < pollingConfigProvider.getMaxRetry() && !checkDistroxInstanceStateIsAvailable(cloudbreakClient, distroxName, hostGroup, desiredState)) {
                LOGGER.info("Waiting for instance status {} in Host Group {} at {} DistroX, ellapsed {}ms", desiredState, hostGroup, distroxName,
                        System.currentTimeMillis() - startTime);
                sleep(pollingConfigProvider.getPollingInterval());
                retryCount++;
            }

            if (checkDistroxInstanceStateIsAvailable(cloudbreakClient, distroxName, hostGroup, desiredState)) {
                Log.log(LOGGER, format(" [%s] host group instance state is [%s] at [%s] DostroX.", hostGroup, desiredState, distroxName));
            } else {
                LOGGER.error("Timeout: Host Group: {} desired instance state: {} is NOT available at {} DostroX during {} retries", hostGroup, desiredState,
                        distroxName, pollingConfigProvider.getMaxRetry());
                throw new TestFailException(" Timeout: Host Group: " + hostGroup + " desired instance state: " + desiredState
                        + " is NOT available at " + distroxName + " DostroX during " + pollingConfigProvider.getMaxRetry() + " retries");
            }
        });

        return distroXTestDto;
    }

    private boolean checkSdxInstanceStateIsAvailable(SdxClient sdxClient, String sdxName, String hostGroup, InstanceStatus desiredState) {
        InstanceMetaDataV4Response instanceMetaDataV4Response = sdxClient.getSdxClient().sdxEndpoint().getDetail(sdxName, new HashSet<>())
                .getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroup))
                .findFirst()
                .orElse(null).getMetadata().stream().findFirst().orElse(null);
        if (instanceMetaDataV4Response != null) {
            LOGGER.info(" Instance group {} with {} group state and with {} instance state is present. ",
                    instanceMetaDataV4Response.getInstanceGroup(),
                    instanceMetaDataV4Response.getState(),
                    instanceMetaDataV4Response.getInstanceStatus()
            );
            return instanceMetaDataV4Response.getInstanceStatus().equals(desiredState);
        } else {
            LOGGER.info(" Instance Metadata is NOT available for {} instance group! ", hostGroup);
            return true;
        }
    }

    private boolean checkDistroxInstanceStateIsAvailable(CloudbreakClient cloudbreakClient, String distroxName, String hostGroup, InstanceStatus desiredState) {
        InstanceMetaDataV4Response instanceMetaDataV4Response = cloudbreakClient.getCloudbreakClient().distroXV1Endpoint()
                .getByName(distroxName, new HashSet<>())
                .getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroup))
                .findFirst()
                .orElse(null).getMetadata().stream().findFirst().orElse(null);
        if (instanceMetaDataV4Response != null) {
            LOGGER.info(" Instance group {} with {} group state and with {} instance state is present. ",
                    instanceMetaDataV4Response.getInstanceGroup(),
                    instanceMetaDataV4Response.getState(),
                    instanceMetaDataV4Response.getInstanceStatus()
            );
            return instanceMetaDataV4Response.getInstanceStatus().equals(desiredState);
        } else {
            LOGGER.info(" Instance Metadata is NOT available for {} instance group! ", hostGroup);
            return true;
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
