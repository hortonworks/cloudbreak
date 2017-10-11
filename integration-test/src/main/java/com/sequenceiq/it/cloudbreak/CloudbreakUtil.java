package com.sequenceiq.it.cloudbreak;

import static java.lang.Boolean.FALSE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.periscope.api.endpoint.HistoryEndpoint;
import com.sequenceiq.periscope.api.model.HistoryJson;
import com.sequenceiq.periscope.client.AutoscaleClient;


public class CloudbreakUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUtil.class);

    private static final int MAX_RETRY = 360;

    private static final int POLLING_INTERVAL = 10000;

    private CloudbreakUtil() {
    }

    public static void checkResponse(String operation, Response response) {
        if (Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
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

    public static WaitResult waitForHostStatusStack(StackV1Endpoint stackV1Endpoint, String stackId, String hostGroup, String desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean found = FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for host status {} in hostgroup {} ...", desiredStatus, hostGroup);
            sleep();
            StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());
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

    public static void checkClusterFailed(StackV1Endpoint stackV1Endpoint, String stackId, String failMessage) {
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        Assert.assertNotEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE);
        Assert.assertTrue(stackResponse.getCluster().getStatusReason().contains(failMessage));
    }

    public static void checkClusterAvailability(StackV1Endpoint stackV1Endpoint, String port, String stackId, String ambariUser, String ambariPassowrd,
            boolean checkAmbari) {
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());

        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE, "The cluster hasn't been started!");
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

    public static void checkClusterStopped(StackV1Endpoint stackV1Endpoint, String port, String stackId, String ambariUser, String ambariPassword) {
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());

        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.STOPPED, "The cluster is not stopped!");
        Assert.assertEquals(stackResponse.getStatus(), Status.STOPPED, "The stack is not stopped!");

        String ambariIp = stackResponse.getCluster().getAmbariServerIp();
        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertFalse(isAmbariRunning(ambariClient), "The Ambari server is running in stopped state!");
    }

    public static boolean isAmbariRunning(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
            return "RUNNING".equals(ambariHealth);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getAmbariIp(StackV1Endpoint stackV1Endpoint, String stackId, IntegrationTestContext itContext) {
        String ambariIp = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_IP_ID);
        if (StringUtils.isEmpty(ambariIp)) {
            StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());
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
            StackV1Endpoint stackV1Endpoint = cloudbreakClient.stackEndpoint();
            try {
                Map<String, Object> statusResult = stackV1Endpoint.status(Long.valueOf(stackId));
                for (String statusPath : desiredStatuses.keySet()) {
                    currentStatuses.put(statusPath, (String) statusResult.get(statusPath));
                }
            } catch (RuntimeException ignore) {
                continue;
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

    public static WaitResult waitForEvent(CloudbreakClient cloudbreakClient, String stackName, String eventType, String eventMessage, long sinceTimeStamp) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean exitCriteria = FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for event type {} and event message contains {} ...", eventType, eventMessage);
            sleep();
            EventEndpoint eventEndpoint = cloudbreakClient.eventEndpoint();
            List<CloudbreakEventsJson> list = eventEndpoint.get(sinceTimeStamp);
            for (CloudbreakEventsJson event : list) {
                if (event.getStackName().equals(stackName) && event.getEventMessage().contains(eventMessage) && event.getEventType().equals(eventType)) {
                    exitCriteria = Boolean.TRUE;
                    break;
                }
            }
            retryCount++;
        } while (!exitCriteria && retryCount < MAX_RETRY);

        LOGGER.info("Event {} for {} happened and event message contains {}", eventType, stackName, eventMessage);

        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    public static WaitResult waitForAutoScalingEvent(AutoscaleClient autoscaleClient, Long clusterId, Long currentTime) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean exitCriteria = FALSE;
        int retryCount = 0;
        do {
            LOGGER.info("Waiting for auto scaling event is success ...");
            sleep();
            HistoryEndpoint historyEndpoint = autoscaleClient.historyEndpoint();
            List<HistoryJson> historyJson = historyEndpoint.getHistory(clusterId);
            for (HistoryJson elem : historyJson) {
                if ((elem.getTimestamp() > currentTime) && "SUCCESS".equals(elem.getScalingStatus().toString())) {
                    exitCriteria = Boolean.TRUE;
                }
            }

            retryCount++;
        } while (!exitCriteria && retryCount < MAX_RETRY);

        LOGGER.info("Auto scaling event happened successfully");

        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    private static boolean checkStatuses(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = true;
        for (Entry<String, String> desiredStatus: desiredStatuses.entrySet()) {
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
        for (Entry<String, String> desiredStatus: currentStatuses.entrySet()) {
            if (failedStatuses.stream().anyMatch(fs -> desiredStatus.getValue().contains(fs))) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean checkNotExpectedDelete(Map<String, String> currentStatuses, Map<String, String> desiredStatuses) {
        boolean result = false;
        for (Entry<String, String> desiredStatus: desiredStatuses.entrySet()) {
            if (!"DELETE_COMPLETED".equals(desiredStatus.getValue()) && "DELETE_COMPLETED".equals(currentStatuses.get(desiredStatus.getKey()))) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static void sleep() {
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait", e);
        }
    }

    private static int getNodeCount(StackResponse stackResponse) {
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getGroup())) {
                nodeCount += instanceGroup.getNodeCount();
            }
        }
        return nodeCount;
    }
}
