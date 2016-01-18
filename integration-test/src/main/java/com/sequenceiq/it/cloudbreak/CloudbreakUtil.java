package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.model.CloudbreakClient;

import groovyx.net.http.HttpResponseException;

public class CloudbreakUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUtil.class);
    private static final int MAX_RETRY = 360;
    private static final int POLLING_INTERVAL = 10000;
    private static final String DEFAULT_AMBARI_PORT = "8080";

    private CloudbreakUtil() {
    }

    public static void waitAndCheckStackStatus(IntegrationTestContext itContext, String stackId, String desiredStatus) throws Exception {
        waitAndCheckStatus(itContext, stackId, desiredStatus, "status");
    }

    public static void waitAndCheckClusterStatus(IntegrationTestContext itContext, String stackId, String desiredStatus) throws Exception {
        waitAndCheckStatus(itContext, stackId, desiredStatus, "clusterStatus");
    }

    public static WaitResult waitForStackStatus(IntegrationTestContext itContext, String stackId, String desiredStatus) throws Exception {
        return waitForStatus(itContext, stackId, desiredStatus, "status");
    }

    public static WaitResult waitForClusterStatus(IntegrationTestContext itContext, String stackId, String desiredStatus) throws Exception {
        return waitForStatus(itContext, stackId, desiredStatus, "clusterStatus");
    }

    public static void checkClusterAvailability(StackEndpoint stackEndpoint, String stackId, String ambariUser, String ambariPassowrd) throws Exception {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        Assert.assertEquals(stackResponse.getCluster().getStatus(), "AVAILABLE", "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), "AVAILABLE", "The stack hasn't been started!");

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");

        AmbariClient ambariClient = new AmbariClient(ambariIp, DEFAULT_AMBARI_PORT, ambariUser, ambariPassowrd);
        Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");
        Assert.assertEquals(ambariClient.getClusterHosts().size(), getNodeCount(stackResponse) - 1,
                "The number of cluster nodes in the stack differs from the number of nodes registered in ambari");
    }

    public static void checkClusterStopped(StackEndpoint stackEndpoint, String stackId, String ambariUser, String ambariPassowrd) throws Exception {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        Assert.assertEquals(stackResponse.getCluster().getStatus(), "STOPPED", "The cluster is not stopped!");
        Assert.assertEquals(stackResponse.getStatus(), "STOPPED", "The stack is not stopped!");

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        AmbariClient ambariClient = new AmbariClient(ambariIp, DEFAULT_AMBARI_PORT, ambariUser, ambariPassowrd);
        Assert.assertFalse(isAmbariRunning(ambariClient), "The Ambari server is running in stopped state!");
    }

    public static boolean isAmbariRunning(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
            if ("RUNNING".equals(ambariHealth)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitAndCheckStatus(IntegrationTestContext itContext, String stackId, String desiredStatus, String statusPath) throws Exception {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatus(itContext, stackId, desiredStatus, statusPath);
            if (waitResult == WaitResult.FAILED) {
                Assert.fail("The stack has failed");
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
        }
    }

    private static WaitResult  waitForStatus(IntegrationTestContext itContext, String stackId, String desiredStatus, String statusPath) throws Exception {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        String stackStatus = null;
        int retryCount = 0;
        do {
            LOGGER.info("Waiting for stack status {}, stack id: {}, current status {} ...", desiredStatus, stackId, stackStatus);
            sleep();
            StackEndpoint stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackEndpoint();
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
            nodeCount += instanceGroup.getNodeCount();
        }
        return nodeCount;
    }
}
