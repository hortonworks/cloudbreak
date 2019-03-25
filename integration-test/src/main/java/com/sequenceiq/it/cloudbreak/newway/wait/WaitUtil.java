package com.sequenceiq.it.cloudbreak.newway.wait;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;

@Component
public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    private static final int MAX_RETRY = 1800;

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
            } catch (RuntimeException ForbiddenException) {
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

    private boolean hasNoExpectedFailure(Status currentStatuses, Status desiredStatuses) {
        return hasNoExpectedStatus(currentStatuses, desiredStatuses);
    }

    private boolean checkNotExpectedDelete(Status currentStatuses, Status desiredStatuses) {
        return hasNoExpectedStatus(currentStatuses, desiredStatuses);
    }

    private boolean hasNoExpectedStatus(Status currentStatus, Status desiredStatus) {
        return DELETE_COMPLETED != desiredStatus && DELETE_COMPLETED == currentStatus;
    }
}
