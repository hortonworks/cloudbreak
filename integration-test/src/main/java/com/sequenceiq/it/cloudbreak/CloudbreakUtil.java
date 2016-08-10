package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;

import groovyx.net.http.HttpResponseException;

public class CloudbreakUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUtil.class);
    private static final int MAX_RETRY = 360;
    private static final int POLLING_INTERVAL = 10000;

    private CloudbreakUtil() {
    }

    public static void checkResponse(String operation, Response response) {
        if (Response.Status.Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            String errormsg = "Error happened during " + operation + " rest operation: status: " + response.getStatus() + ", error: "
                    + response.readEntity(String.class);
            LOGGER.error(errormsg);
            throw new RuntimeException(errormsg);
        }
    }

    public static void waitAndCheckStackStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        waitAndCheckStatus(cloudbreakClient, stackId, desiredStatus, "status");
    }

    public static void waitAndCheckClusterStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        waitAndCheckStatus(cloudbreakClient, stackId, desiredStatus, "clusterStatus");
    }

    public static WaitResult waitForStackStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        return waitForStatus(cloudbreakClient, stackId, desiredStatus, "status");
    }

    public static WaitResult waitForClusterStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        return waitForStatus(cloudbreakClient, stackId, desiredStatus, "clusterStatus");
    }

    public static void checkClusterFailed(StackEndpoint stackEndpoint, String stackId, String failMessage) {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));
        Assert.assertEquals(stackResponse.getCluster().getStatus(), "CREATE_FAILED");
        Assert.assertTrue(stackResponse.getCluster().getStatusReason().contains(failMessage));
    }

    public static void checkClusterAvailability(StackEndpoint stackEndpoint, String port, String stackId, String ambariUser, String ambariPassowrd,
            boolean checkAmbari) {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        Assert.assertEquals(stackResponse.getCluster().getStatus(), "AVAILABLE", "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");

        if (checkAmbari) {
            AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassowrd);
            Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");
            Assert.assertEquals(ambariClient.getClusterHosts().size(), getNodeCount(stackResponse),
                    "The number of cluster nodes in the stack differs from the number of nodes registered in ambari");
        }
    }

    public static void checkClusterStopped(StackEndpoint stackEndpoint, String port, String stackId, String ambariUser, String ambariPassword) {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        Assert.assertEquals(stackResponse.getCluster().getStatus(), "STOPPED", "The cluster is not stopped!");
        Assert.assertEquals(stackResponse.getStatus(), Status.STOPPED, "The stack is not stopped!");

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertFalse(isAmbariRunning(ambariClient), "The Ambari server is running in stopped state!");
    }

    public static boolean isAmbariRunning(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
            return "RUNNING".equals(ambariHealth);
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitAndCheckStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus, String statusPath) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatus(cloudbreakClient, stackId, desiredStatus, statusPath);
            if (waitResult == WaitResult.FAILED) {
                Assert.fail("The stack has failed");
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
        }
    }

    private static WaitResult waitForStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus, String statusPath) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        String stackStatus = null;
        int retryCount = 0;
        do {
            LOGGER.info("Waiting for stack status {}, stack id: {}, current status {} ...", desiredStatus, stackId, stackStatus);
            sleep();
            StackEndpoint stackEndpoint = cloudbreakClient.stackEndpoint();
            try {
                Map<String, Object> statusResult = stackEndpoint.status(Long.valueOf(stackId));
                stackStatus = (String) statusResult.get(statusPath);
            } catch (Exception exception) {
                if (exception instanceof HttpResponseException && ((HttpResponseException) exception).getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                    stackStatus = "DELETE_COMPLETED";
                } else {
                    continue;
                }
            }
            retryCount++;
        } while (!desiredStatus.equals(stackStatus) && !stackStatus.contains("FAILED") && !"DELETE_COMPLETED".equals(stackStatus) && retryCount < MAX_RETRY);
        LOGGER.info("Status {} for {} is in desired status {}", statusPath, stackId, stackStatus);
        if (stackStatus.contains("FAILED") || (!"DELETE_COMPLETED".equals(desiredStatus) && "DELETE_COMPLETED".equals(stackStatus))) {
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

    private static int getNodeCount(StackResponse stackResponse) {
        List<InstanceGroupJson> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupJson instanceGroup : instanceGroups) {
            if (!instanceGroup.getGroup().equals("cbgateway")) {
                nodeCount += instanceGroup.getNodeCount();
            }
        }
        return nodeCount;
    }
}
