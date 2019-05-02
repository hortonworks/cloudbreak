package com.sequenceiq.it.cloudbreak.newway.wait;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;

@Component
public class WaitUtilForMultipleStatuses {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtilForMultipleStatuses.class);

    @Value("${integrationtest.testsuite.maxRetry:1800}")
    private int maxRetry;

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    public Map<String, String> waitAndCheckStatuses(CloudbreakClient cloudbreakClient, String stackName, Map<String, Status> desiredStatuses,
            long pollingInterval) {
        Map<String, String> errors = new HashMap<>();
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(cloudbreakClient, stackName, desiredStatuses, Math.max(this.pollingInterval, pollingInterval));
            if (waitResult == WaitResult.FAILED || waitResult == WaitResult.TIMEOUT) {
                break;
            }
        }
        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("The stack has failed: ").append(System.lineSeparator());
            StackStatusV4Response status = cloudbreakClient.getCloudbreakClient().stackV4Endpoint()
                    .getStatusByName(cloudbreakClient.getWorkspaceId(), stackName);
            if (status != null) {
                desiredStatuses.forEach((key, value) -> {
                    Status subStatus = getStatus(status, key);
                    if (subStatus != null) {
                        errors.put(key, subStatus.name());
                    }
                });

                errors.forEach((key, value) -> {
                    builder.append(key).append(": ").append(value).append(System.lineSeparator());
                });
                builder.append("statusReason: ").append(status.getStatusReason());
            }
            throw new RuntimeException(builder.toString());
        } else if (waitResult == WaitResult.TIMEOUT) {
            throw new RuntimeException("Timeout happened");
        } else if (!Status.DELETE_COMPLETED.equals(desiredStatuses.get("status"))) {
            StackStatusV4Response status = cloudbreakClient.getCloudbreakClient().stackV4Endpoint()
                    .getStatusByName(cloudbreakClient.getWorkspaceId(), stackName);
            if (status != null) {
                String statusReason = status.getStatusReason();
                if (statusReason != null) {
                    errors.put("statusReason", statusReason);
                }
            }
        }
        return errors;
    }

    private Status getStatus(StackStatusV4Response status, String fieldName) {
        Status result = null;
        try {
            Field field = status.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            result = (Status) field.get(status);
            field.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {

        }
        return result;
    }

    private WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, String stackName, Map<String, Status> desiredStatuses, long pollingInterval) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, Status> currentStatuses = new HashMap<>();

        int retryCount = 0;
        while (!checkStatuses(currentStatuses, desiredStatuses) && !checkFailedStatuses(currentStatuses) && retryCount < maxRetry) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatuses, stackName, currentStatuses);

            sleep(pollingInterval);
            try {
                StackStatusV4Response status = cloudbreakClient.getCloudbreakClient().stackV4Endpoint()
                        .getStatusByName(cloudbreakClient.getWorkspaceId(), stackName);
                for (String statusPath : desiredStatuses.keySet()) {
                    Status currStatus = Status.DELETE_COMPLETED;
                    if (status != null) {
                        currStatus = getStatus(status, statusPath);
                    }
                    currentStatuses.put(statusPath, currStatus);
                }
            } catch (RuntimeException ignore) {
                if (ignore instanceof ForbiddenException) {
                    desiredStatuses.entrySet().stream()
                            .filter(entry -> Status.DELETE_COMPLETED.equals(entry.getValue()))
                            .map(Map.Entry::getKey)
                            .forEach(statusPath -> currentStatuses.put(statusPath, Status.DELETE_COMPLETED));
                    break;
                }
                continue;
            }

            retryCount++;
        }

        if (currentStatuses.values().stream().anyMatch(cs -> cs.name().contains("FAILED")) && hasNoExpectedFailure(currentStatuses, desiredStatuses)
                || checkNotExpectedDelete(currentStatuses, desiredStatuses)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatuses, stackName, currentStatuses);
        } else if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatuses, stackName, currentStatuses);
        } else {
            LOGGER.info("{} are in desired status(es) {}", stackName, currentStatuses);
        }
        return waitResult;
    }

    private void sleep(long pollingInterval) {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait", e);
        }
    }

    private boolean checkStatuses(Map<String, Status> currentStatuses, Map<String, Status> desiredStatuses) {
        boolean result = true;
        for (Map.Entry<String, Status> desiredStatus : desiredStatuses.entrySet()) {
            if (!desiredStatus.getValue().equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean checkFailedStatuses(Map<String, Status> currentStatuses) {
        boolean result = false;
        List<String> failedStatuses = Arrays.asList("FAILED", "DELETE_COMPLETED");
        for (Map.Entry<String, Status> desiredStatus : currentStatuses.entrySet()) {
            if (failedStatuses.stream().anyMatch(fs -> desiredStatus.getValue().name().contains(fs))) {
                LOGGER.info("In FAILED status: {}", currentStatuses);
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean hasNoExpectedFailure(Map<String, Status> currentStatuses, Map<String, Status> desiredStatuses) {
        return hasNoExpectedStatus(Status.CREATE_FAILED, currentStatuses, desiredStatuses);
    }

    private boolean checkNotExpectedDelete(Map<String, Status> currentStatuses, Map<String, Status> desiredStatuses) {
        return hasNoExpectedStatus(Status.DELETE_COMPLETED, currentStatuses, desiredStatuses);
    }

    private boolean hasNoExpectedStatus(Status expectedState, Map<String, Status> currentStatuses, Map<String, Status> desiredStatuses) {
        boolean result = false;
        for (Map.Entry<String, Status> desiredStatus : desiredStatuses.entrySet()) {
            if (!expectedState.equals(desiredStatus.getValue()) && expectedState.equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = true;
                break;
            }
        }
        return result;
    }
}
