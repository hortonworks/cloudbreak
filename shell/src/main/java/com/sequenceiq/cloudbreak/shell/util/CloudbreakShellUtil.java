package com.sequenceiq.cloudbreak.shell.util;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;

@Component
public class CloudbreakShellUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakShellUtil.class);
    private static final int MAX_RETRY = 360;
    private static final int POLLING_INTERVAL = 10000;
    private static final int MAX_ATTEMPT = 3;

    @Inject
    private CloudbreakClient cloudbreakClient;

    public void checkResponse(String operation, Response response) {
        if (Response.Status.Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            String errormsg = "Error happened during " + operation + " rest operation: status: " + response.getStatus() + ", error: "
                    + response.readEntity(String.class);
            LOGGER.error(errormsg);
            throw new RuntimeException(errormsg);
        }
    }

    public WaitResult waitAndCheckStackStatus(Long stackId, String desiredStatus) throws Exception {
        return waitAndCheckStatus(stackId, desiredStatus, "status");
    }

    public WaitResult waitAndCheckClusterStatus(Long stackId, String desiredStatus) throws Exception {
        return waitAndCheckStatus(stackId, desiredStatus, "clusterStatus");
    }

    private WaitResult waitAndCheckStatus(Long stackId, String desiredStatus, String statusPath) throws Exception {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            WaitResult waitResult = waitForStatus(stackId, desiredStatus, statusPath);
            if (waitResult.equals(WaitResult.FAILED) || waitResult.equals(WaitResult.TIMEOUT)) {
                return WaitResult.FAILED;
            }
        }
        return WaitResult.SUCCESSFUL;
    }

    private WaitResult waitForStatus(Long stackId, String desiredStatus, String statusPath) throws Exception {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        String stackStatus = null;
        int retryCount = 0;
        do {
            LOGGER.info("Waiting for stack status {}, stack id: {}, current status {} ...", desiredStatus, stackId, stackStatus);
            sleep();
            Map<String, Object> statusResult = cloudbreakClient.stackEndpoint().status(stackId);
            if (statusResult == null || statusResult.isEmpty()) {
                return WaitResult.FAILED;
            }
            stackStatus = (String) statusResult.get(statusPath);
            retryCount++;
        } while (!desiredStatus.equals(stackStatus) && !stackStatus.contains("FAILED") && !Status.DELETE_COMPLETED.name().equals(stackStatus)
                && retryCount < MAX_RETRY);
        LOGGER.info("Status {} for {} is in desired status {}", statusPath, stackId, stackStatus);
        if (stackStatus.contains("FAILED") || (!Status.DELETE_COMPLETED.name().equals(desiredStatus) && Status.DELETE_COMPLETED.name().equals(stackStatus))) {
            waitResult = WaitResult.FAILED;
        }
        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private static void sleep() {
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait: {}", e);
        }
    }

    public enum WaitResult {
        SUCCESSFUL,
        FAILED,
        TIMEOUT
    }
}
