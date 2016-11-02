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

    public WaitResult waitAndCheckStackStatus(Long stackId, String desiredStatus) {
        return waitAndCheckStatus(stackId, desiredStatus, "status");
    }

    public WaitResult waitAndCheckClusterStatus(Long stackId, String desiredStatus) {
        return waitAndCheckStatus(stackId, desiredStatus, "clusterStatus");
    }

    private WaitResult waitAndCheckStatus(Long stackId, String desiredStatus, String statusPath) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            WaitResult waitResult = waitForStatus(stackId, desiredStatus, statusPath);
            if (waitResult.getWaitResultStatus().equals(WaitResultStatus.FAILED)) {
                return waitResult;
            }
        }
        return new WaitResult(WaitResultStatus.SUCCESSFUL, "");
    }

    private WaitResult waitForStatus(Long stackId, String desiredStatus, String statusPath) {
        WaitResult waitResult = new WaitResult(WaitResultStatus.SUCCESSFUL, "");
        String status = null;
        String statusReason;
        int retryCount = 0;
        do {
            LOGGER.info("Waiting for status {}, stack id: {}, current status {} ...", desiredStatus, stackId, status);
            sleep();
            Map<String, Object> statusResult = cloudbreakClient.stackEndpoint().status(stackId);
            if (statusResult == null || statusResult.isEmpty()) {
                return new WaitResult(WaitResultStatus.FAILED, "Status result is empty.");
            }
            status = (String) statusResult.get(statusPath);
            statusReason = (String) statusResult.get(statusPath + "Reason");
            retryCount++;
        } while (!desiredStatus.equals(status) && !status.contains("FAILED") && !Status.DELETE_COMPLETED.name().equals(status)
                && retryCount < MAX_RETRY);
        LOGGER.info("Status {} for {} is in desired status {}", statusPath, stackId, status);
        if (status.contains("FAILED") || (!Status.DELETE_COMPLETED.name().equals(desiredStatus) && Status.DELETE_COMPLETED.name().equals(status))) {
            waitResult = new WaitResult(WaitResultStatus.FAILED, statusReason);
        } else if (retryCount == MAX_RETRY) {
            waitResult = new WaitResult(WaitResultStatus.FAILED, "Timeout while trying to fetch status.");
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

    public enum WaitResultStatus {
        SUCCESSFUL,
        FAILED
    }

    public class WaitResult {

        private WaitResultStatus waitResultStatus;

        private String reason;

        public WaitResult(WaitResultStatus waitResultStatus, String reason) {
            this.waitResultStatus = waitResultStatus;
            this.reason = reason;
        }

        public WaitResultStatus getWaitResultStatus() {
            return waitResultStatus;
        }

        public String getReason() {
            return reason;
        }
    }
}
