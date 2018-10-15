package com.sequenceiq.it.cloudbreak.newway.wait;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;

@Component
public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    private static final int MAX_RETRY = 360;

    @Value("${integrationtest.testsuite.pollingInterval}")
    private long pollingInterval;

    public Map<String, String> waitAndCheckStatuses(CloudbreakClient cloudbreakClient, String stackName, Map<String, String> desiredStatuses) {
        Map<String, String> ret = new HashMap<>();
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        for (int retryBecauseOfWrongStatusHandlingInCB = 0; retryBecauseOfWrongStatusHandlingInCB < 3; retryBecauseOfWrongStatusHandlingInCB++) {
            waitResult = waitForStatuses(cloudbreakClient, stackName, desiredStatuses);
        }
        if (waitResult == WaitResult.FAILED) {
            StringBuilder builder = new StringBuilder("The stack has failed: ").append(System.lineSeparator());
            Map<String, Object> statusByNameInWorkspace = cloudbreakClient.getCloudbreakClient().stackV3Endpoint()
                    .getStatusByNameInWorkspace(cloudbreakClient.getWorkspaceId(), stackName);
            if (statusByNameInWorkspace != null) {
                desiredStatuses.forEach((key, value) -> {
                    Object o = statusByNameInWorkspace.get(key);
                    if (o != null) {
                        ret.put(key, o.toString());
                    }
                });

                ret.forEach((key, value) -> {
                    builder.append(key).append(',').append(value).append(System.lineSeparator());
                });
                builder.append("statusReason: ").append(statusByNameInWorkspace.get("statusReason"));
            }
            throw new RuntimeException(builder.toString());
        } else if (waitResult == WaitResult.TIMEOUT) {
            throw new RuntimeException("Timeout happened");
        } else {
            Map<String, Object> statusByNameInWorkspace = cloudbreakClient.getCloudbreakClient()
                    .stackV3Endpoint().getStatusByNameInWorkspace(cloudbreakClient.getWorkspaceId(), stackName);
            if (statusByNameInWorkspace != null) {
                Object statusReason = statusByNameInWorkspace.get("statusReason");
                if (statusReason != null) {
                    ret.put("statusReason", statusReason.toString());
                }
            }
        }
        return ret;
    }

    private WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, String stackName, Map<String, String> desiredStatuses) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, String> currentStatuses = new HashMap<>();

        int retryCount = 0;
        while (!checkStatuses(currentStatuses, desiredStatuses) && !checkFailedStatuses(currentStatuses) && retryCount < MAX_RETRY) {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatuses, stackName, currentStatuses);

            sleep();
            StackV3Endpoint stackV3Endpoint = cloudbreakClient.getCloudbreakClient().stackV3Endpoint();
            try {
                Map<String, Object> statusResult = stackV3Endpoint.getStatusByNameInWorkspace(cloudbreakClient.getWorkspaceId(), stackName);
                for (String statusPath : desiredStatuses.keySet()) {
                    String currStatus = "DELETE_COMPLETED";
                    if (!CollectionUtils.isEmpty(statusResult)) {
                        currStatus = (String) statusResult.get(statusPath);
                    }
                    currentStatuses.put(statusPath, currStatus);
                }
            } catch (RuntimeException ignore) {
                if (ignore instanceof ForbiddenException) {
                    desiredStatuses.entrySet().stream()
                            .filter(entry -> "DELETE_COMPLETED".equals(entry.getValue()))
                            .map(Map.Entry::getKey)
                            .forEach(statusPath -> currentStatuses.put(statusPath, "DELETE_COMPLETED"));
                }
                continue;
            }

            retryCount++;
        }

        if (currentStatuses.values().stream().anyMatch(cs -> cs.contains("FAILED")) && hasNoExpectedFailure(currentStatuses, desiredStatuses)
                || checkNotExpectedDelete(currentStatuses, desiredStatuses)) {
            waitResult = WaitResult.FAILED;
            LOGGER.info("Desired status(es) are {} for {} but status(es) are {}", desiredStatuses, stackName, currentStatuses);
        } else if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
            LOGGER.info("Timeout: Desired tatus(es) are {} for {} but status(es) are {}", desiredStatuses, stackName, currentStatuses);
        } else {
            LOGGER.info("{} are in desired status(es) {}", stackName, currentStatuses);
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

    private boolean checkStatuses(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = true;
        for (Map.Entry<String, String> desiredStatus : desiredStatuses.entrySet()) {
            if (!desiredStatus.getValue().equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean checkFailedStatuses(Map<String, String> currentStatuses) {
        boolean result = false;
        List<String> failedStatuses = Arrays.asList("FAILED", "DELETE_COMPLETED");
        for (Map.Entry<String, String> desiredStatus : currentStatuses.entrySet()) {
            if (failedStatuses.stream().anyMatch(fs -> desiredStatus.getValue().contains(fs))) {
                LOGGER.info("In FAILED status: {}", currentStatuses);
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean hasNoExpectedFailure(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        return hasNoExpectedStatus("CREATE_FAILED", currentStatuses, desiredStatuses);
    }

    private boolean checkNotExpectedDelete(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        return hasNoExpectedStatus("DELETE_COMPLETED", currentStatuses, desiredStatuses);
    }

    private boolean hasNoExpectedStatus(String expectedState, Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = false;
        for (Map.Entry<String, String> desiredStatus : desiredStatuses.entrySet()) {
            if (!expectedState.equals(desiredStatus.getValue()) && expectedState.equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = true;
                break;
            }
        }
        return result;
    }
}
