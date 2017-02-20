package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;

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
        waitAndCheckStatuses(cloudbreakClient, stackId, Collections.singletonMap("status", desiredStatus));
    }

    public static void waitAndCheckClusterStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        waitAndCheckStatuses(cloudbreakClient, stackId, Collections.singletonMap("clusterStatus", desiredStatus));
    }

    public static WaitResult waitForStackStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, stackId, Collections.singletonMap("status", desiredStatus));
    }

    public static WaitResult waitForClusterStatus(CloudbreakClient cloudbreakClient, String stackId, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, stackId, Collections.singletonMap("clusterStatus", desiredStatus));
    }

    public static void waitAndCheckStatuses(CloudbreakClient cloudbreakClient, String stackId, Map<String, String> desiredStatuses) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, stackId, desiredStatuses);
            if (waitResult == WaitResult.FAILED) {
                Assert.fail("The stack has failed");
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
        }
    }

    public static WaitResult waitForHostStatusStack(StackEndpoint stackEndpoint, String stackId, String hostGroup, String desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean found = Boolean.FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for host status {} in hostgroup {} ...", desiredStatus, hostGroup);
            sleep();
            StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));
            Set<HostGroupResponse> hostGroupResponse = stackResponse.getCluster().getHostGroups();
            for (HostGroupResponse hr : hostGroupResponse) {
                if (hr.getName().equals(hostGroup)) {
                    Set<HostMetadataResponse> hostMetadataResponses = hr.getMetadata();
                    for (HostMetadataResponse hmr : hostMetadataResponses) {
                        if (hmr.getState().equals(desiredStatus)) {
                            found = Boolean.TRUE;
                        }
                    }
                }
            }

            retryCount++;

        } while (!found && (retryCount < MAX_RETRY));

        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
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

    public static String getAmbariIp(StackEndpoint stackEndpoint, String stackId, IntegrationTestContext itContext) {
        String ambariIp = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_IP_ID);
        if (StringUtils.isEmpty(ambariIp)) {
            StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));
            ambariIp = stackResponse.getCluster().getAmbariServerIp();
            Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");
            itContext.putContextParam(CloudbreakITContextConstants.AMBARI_IP_ID, ambariIp);
        }
        return ambariIp;
    }

    private static WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, String stackId, Map<String, String> desiredStatuses) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, String> currentStatuses = new HashMap<>();

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatuses, stackId, currentStatuses);

            sleep();
            StackEndpoint stackEndpoint = cloudbreakClient.stackEndpoint();
            try {
                Map<String, Object> statusResult = stackEndpoint.status(Long.valueOf(stackId));
                for (String statusPath : desiredStatuses.keySet()) {
                    currentStatuses.put(statusPath, (String) statusResult.get(statusPath));
                }
            } catch (Exception exception) {
                if (exception instanceof HttpResponseException && ((HttpResponseException) exception).getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                    for (String statusPath : desiredStatuses.keySet()) {
                        currentStatuses.put(statusPath, "DELETE_COMPLETED");
                    }
                } else {
                    continue;
                }
            }

            retryCount++;
        } while (!checkStatuses(currentStatuses, desiredStatuses) && !checkFailedStatuses(currentStatuses) && retryCount < MAX_RETRY);

        LOGGER.info("Status(es) {} for {} are in desired status(es) {}", desiredStatuses.keySet(), stackId, currentStatuses.values());
        if (currentStatuses.containsValue("FAILED") || checkNotExpectedDelete(currentStatuses, desiredStatuses)) {
            waitResult = WaitResult.FAILED;
        }
        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private static boolean checkStatuses(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = true;
        for (Map.Entry<String, String> desiredStatus: desiredStatuses.entrySet()) {
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
        for (Map.Entry<String, String> desiredStatus: currentStatuses.entrySet()) {
            if (failedStatuses.contains(desiredStatus.getValue())) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean checkNotExpectedDelete(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = false;
        for (Map.Entry<String, String> desiredStatus: desiredStatuses.entrySet()) {
            if (!desiredStatus.getValue().equals("DELETE_COMPLETED") && currentStatuses.get(desiredStatus.getKey()).equals("DELETE_COMPLETED")) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static void sleep() {
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait: {}", e);
        }
    }

    private static int getNodeCount(StackResponse stackResponse) {
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (!instanceGroup.getGroup().equals("cbgateway")) {
                nodeCount += instanceGroup.getNodeCount();
            }
        }
        return nodeCount;
    }
}
