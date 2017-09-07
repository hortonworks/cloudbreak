package com.sequenceiq.cloudbreak.shell.util;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

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

    private static final int MILISECOND = 1000;

    @Inject
    private CloudbreakClient cloudbreakClient;

    public void checkResponse(String operation, Response response) {
        if (Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            String errormsg = "Error happened during " + operation + " rest operation: status: " + response.getStatus() + ", error: "
                    + response.readEntity(String.class);
            LOGGER.error(errormsg);
            throw new RuntimeException(errormsg);
        }
    }

    public WaitResult waitAndCheckStackStatus(Long stackId, String desiredStatus, Long timeout) {
        return waitAndCheckStatus(stackId, desiredStatus, "status", timeout);
    }

    public WaitResult waitAndCheckClusterStatus(Long stackId, String desiredStatus, Long timeout) {
        return waitAndCheckStatus(stackId, desiredStatus, "clusterStatus", timeout);
    }

    private WaitResult waitAndCheckStatus(Long stackId, String desiredStatus, String statusPath, Long timeout) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            WaitResult waitResult = waitForStatus(stackId, desiredStatus, statusPath, timeout);
            if (waitResult.getWaitResultStatus().equals(WaitResultStatus.FAILED)) {
                return waitResult;
            }
        }
        return new WaitResult(WaitResultStatus.SUCCESSFUL, "");
    }

    private WaitResult waitForStatus(Long stackId, String desiredStatus, String statusPath, Long timeout) {
        WaitResult waitResult = new WaitResult(WaitResultStatus.SUCCESSFUL, "");
        String status = null;
        String statusReason;
        int retryCount = 0;
        long fullTime = 0;
        do {
            Date start = new Date();
            LOGGER.info("Waiting for status {}, stack id: {}, current status {} ...", desiredStatus, stackId, status);
            sleep();
            Map<String, Object> statusResult = cloudbreakClient.stackEndpoint().status(stackId);
            if (statusResult == null || statusResult.isEmpty()) {
                return new WaitResult(WaitResultStatus.FAILED, "Status result is empty.");
            }
            status = (String) statusResult.get(statusPath);
            statusReason = (String) statusResult.get(statusPath + "Reason");
            retryCount++;
            Date end = new Date();
            fullTime += (end.getTime() - start.getTime()) / MILISECOND;
        } while (!desiredStatus.equals(status) && !status.contains("FAILED") && !Status.DELETE_COMPLETED.name().equals(status)
                && shouldNotExitFromPolling(retryCount, timeout, fullTime));
        LOGGER.info("Status {} for {} is in desired status {}", statusPath, stackId, status);
        if (status.contains("FAILED") || (!Status.DELETE_COMPLETED.name().equals(desiredStatus) && Status.DELETE_COMPLETED.name().equals(status))) {
            waitResult = new WaitResult(WaitResultStatus.FAILED, statusReason);
        } else if (retryCount == MAX_RETRY || timeout != null && fullTime >= timeout) {
            waitResult = new WaitResult(WaitResultStatus.FAILED, "Timeout while trying to fetch status.");
        }
        return waitResult;
    }

    private boolean shouldNotExitFromPolling(int retryCount, Long timeout, Long fullTime) {
        if (timeout == null) {
            return retryCount < MAX_RETRY;
        }
        return fullTime < timeout;
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

    public static class WaitResult {

        private final WaitResultStatus waitResultStatus;

        private final String reason;

        private WaitResult(WaitResultStatus waitResultStatus, String reason) {
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
