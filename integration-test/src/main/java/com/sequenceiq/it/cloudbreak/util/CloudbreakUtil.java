package com.sequenceiq.it.cloudbreak.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.wait.PollingConfigProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class CloudbreakUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUtil.class);

    @Inject
    private PollingConfigProvider pollingConfigProvider;

    private static int maxRetry;

    private static long pollingInterval;

    private CloudbreakUtil() {
    }

    public static WaitResult waitForStackStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, workspaceId, stackName, Collections.singletonMap("status", desiredStatus));
    }

    private static WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName,
            Map<String, String> desiredStatuses) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, String> currentStatuses = new HashMap<>();

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatuses, stackName, currentStatuses);

            sleep();
            StackV4Endpoint stackV4Endpoint = cloudbreakClient.stackV4Endpoint();
            try {
                StackStatusV4Response statusResult = stackV4Endpoint.getStatusByName(workspaceId, stackName);
                for (String statusPath : desiredStatuses.keySet()) {
                    currentStatuses.put(statusPath, statusResult.getStatus().name());
                }
            } catch (RuntimeException ignore) {
                continue;
            }

            retryCount++;
        }
        while (!checkStatuses(currentStatuses, desiredStatuses) && !checkFailedStatuses(currentStatuses) && retryCount < maxRetry);

        LOGGER.info("Status(es) {} for {} are in desired status(es) {}", desiredStatuses.keySet(), stackName, currentStatuses.values());
        if (currentStatuses.values().stream().anyMatch(cs -> cs.contains("FAILED")) || checkNotExpectedDelete(currentStatuses, desiredStatuses)) {
            waitResult = WaitResult.FAILED;
        }
        if (retryCount == maxRetry) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private static boolean checkStatuses(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = true;
        for (Entry<String, String> desiredStatus : desiredStatuses.entrySet()) {
            if (!desiredStatus.getValue().equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = false;
                break;
            }
        }
        return result;
    }

    private static boolean checkFailedStatuses(Map<String, String> currentStatuses) {
        boolean result = false;
        List<String> failedStatuses = Arrays.asList("FAILED", "DELETE_COMPLETED");
        for (Entry<String, String> desiredStatus : currentStatuses.entrySet()) {
            if (failedStatuses.stream().anyMatch(fs -> desiredStatus.getValue().contains(fs))) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean checkNotExpectedDelete(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = false;
        for (Entry<String, String> desiredStatus : desiredStatuses.entrySet()) {
            if (!"DELETE_COMPLETED".equals(desiredStatus.getValue()) && "DELETE_COMPLETED".equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static void sleep() {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait", e);
        }
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setPollingInterval() {
        this.pollingInterval = pollingConfigProvider.getPollingInterval();
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setMexRetry() {
        this.maxRetry = pollingConfigProvider.getMaxRetry();
    }
}