package com.sequenceiq.it.cloudbreak.util.wait;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CORRUPTED;

import java.util.HashMap;
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
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;

@Component
public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    private static final int MAX_RETRY = 1800;

    private static final com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status FREE_IPA_DELETE_COMPLETED =
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

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
        while (!checkStatuses(currentStatus, desiredStatus) && !checkFailedStatuses(currentStatus) && retryCount < MAX_RETRY) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, stackName, currentStatus);

            sleep();
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
        } else if (retryCount == MAX_RETRY) {
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
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < MAX_RETRY) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, environmentCrn, currentStatus);

            sleep();
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
        } else if (retryCount == MAX_RETRY) {
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
        while (currentStatus != desiredStatus && !checkFailedStatuses(currentStatus) && retryCount < MAX_RETRY) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, name, currentStatus);

            sleep();
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
        } else if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatus, name, currentStatus);
        } else {
            LOGGER.info("{} are in desired status(es) {}", name, currentStatus);
        }
        return waitResult;
    }

    private void sleep() {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait", e);
        }
    }

    private boolean checkStatuses(Status currentStatus, Status desiredStatus) {
        return desiredStatus == currentStatus;
    }

    private boolean checkFailedStatuses(Status currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == DELETE_COMPLETED;
    }

    private boolean checkFailedStatuses(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status currentStatus) {
        return currentStatus.name().contains("FAILED") || currentStatus == com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
    }

    private boolean checkFailedStatuses(EnvironmentStatus currentStatus) {
        return CORRUPTED.equals(currentStatus) || currentStatus == EnvironmentStatus.ARCHIVED;
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
}
