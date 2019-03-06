package com.sequenceiq.it.cloudbreak;

import static java.lang.Boolean.FALSE;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.client.AutoscaleClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class CloudbreakUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUtil.class);

    private static final int MAX_RETRY = 360;

    private static long pollingInterval = 10000L;

    private CloudbreakUtil() {
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Value("${integrationtest.testsuite.pollingInterval:10000}")
    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public static void checkResponse(String operation, Response response) {
        if (Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            String errormsg = "Error happened during " + operation + " rest operation: status: " + response.getStatus() + ", error: "
                    + response.readEntity(String.class);
            LOGGER.error(errormsg);
            throw new RuntimeException(errormsg);
        }
    }

    public static void waitAndCheckStackStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, String desiredStatus) {
        waitAndCheckStatuses(cloudbreakClient, workspaceId, stackName, Collections.singletonMap("status", desiredStatus));
    }

    public static void waitAndCheckClusterStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, String desiredStatus) {
        waitAndCheckStatuses(cloudbreakClient, workspaceId, stackName, Collections.singletonMap("clusterStatus", desiredStatus));
    }

    public static WaitResult waitForStackStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, workspaceId, stackName, Collections.singletonMap("status", desiredStatus));
    }

    public static WaitResult waitForClusterStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, String desiredStatus) {
        return waitForStatuses(cloudbreakClient, workspaceId, stackName, Collections.singletonMap("clusterStatus", desiredStatus));
    }

    public static void waitAndCheckStatuses(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Map<String, String> desiredStatuses) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, workspaceId, stackName, desiredStatuses);
            if (waitResult == WaitResult.FAILED) {
                Assert.fail("The stack has failed");
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
        }
    }

    public static WaitResult waitForHostStatusStack(StackV4Endpoint stackV4Endpoint, Long workspaceId, String stackName, String hostGroup,
            String desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean found = FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for host status {} in hostgroup {} ...", desiredStatus, hostGroup);
            sleep();
            StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
            List<InstanceGroupV4Response> hostGroupResponse = stackResponse.getInstanceGroups();
            for (InstanceGroupV4Response hr : hostGroupResponse) {
                if (hr.getName().equals(hostGroup)) {
                    Set<InstanceMetaDataV4Response> hostMetadataResponses = hr.getMetadata();
                    for (InstanceMetaDataV4Response hmr : hostMetadataResponses) {
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

    public static void checkClusterFailed(StackV4Endpoint stackV4Endpoint, Long workspaceId, String stackName, CharSequence failMessage) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        Assert.assertNotEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE);
        Assert.assertTrue(stackResponse.getCluster().getStatusReason().contains(failMessage));
    }

    public static void checkClusterAvailability(StackV4Endpoint stackV4Endpoint, String port, Long workspaceId, String stackName, String ambariUser,
            String ambariPassowrd,
            boolean checkAmbari) throws IOException, URISyntaxException {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkClusterAvailability(stackResponse, port, ambariUser, ambariPassowrd, checkAmbari);
    }

    public static void checkClusterAvailabilityThroughGateway(StackV4Endpoint stackV4Endpoint, Long workspaceId, String stackName) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkStackStatusForClusterAvailability(stackResponse);

        String ambariServerUrl = stackResponse.getCluster().getServerUrl();
        Assert.assertNotNull(ambariServerUrl, "The Ambari URL is not available!");
        Response response = RestClientUtil.get().target(ambariServerUrl).request().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.getStatus(), "Ambari is not available!");
    }

    private static void checkClusterAvailability(StackV4Response stackResponse, String port, String ambariUser, String ambariPassowrd,
            boolean checkAmbari) throws IOException, URISyntaxException {
        checkStackStatusForClusterAvailability(stackResponse);

        String ambariIp = stackResponse.getCluster().getServerIp();
        Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");

        if (checkAmbari) {
            AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassowrd);
            Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");
            Assert.assertEquals(ambariClient.getClusterHosts().size(), getNodeCount(stackResponse),
                    "The number of cluster nodes in the stack differs from the number of nodes registered in ambari");
        }
    }

    private static void checkStackStatusForClusterAvailability(StackV4Response stackResponse) {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE, "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
    }

    public static void checkClusterStopped(StackV4Endpoint stackV4Endpoint, String port, Long workspaceId, String stackName, String ambariUser,
            String ambariPassword) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkClusterStopped(port, ambariUser, ambariPassword, stackResponse);
    }

    private static void checkClusterStopped(String port, String ambariUser, String ambariPassword, StackV4Response stackResponse) {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.STOPPED, "The cluster is not stopped!");
        Assert.assertEquals(stackResponse.getStatus(), Status.STOPPED, "The stack is not stopped!");

        String ambariIp = stackResponse.getCluster().getServerIp();
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

    public static String getAmbariIp(StackV4Endpoint stackV4Endpoint, Long workspaceId, String stackName, IntegrationTestContext itContext) {
        String ambariIp = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_IP_ID);
        if (StringUtils.isEmpty(ambariIp)) {
            StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
            ambariIp = stackResponse.getCluster().getServerIp();
            Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");
            itContext.putContextParam(CloudbreakITContextConstants.AMBARI_IP_ID, ambariIp);
        }
        return ambariIp;
    }

    public static String getAmbariIp(StackV4Response stackResponse, IntegrationTestContext itContext) {
        String ambariIp = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_IP_ID);
        if (StringUtils.isEmpty(ambariIp)) {
            ambariIp = stackResponse.getCluster().getServerIp();
            Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");
            itContext.putContextParam(CloudbreakITContextConstants.AMBARI_IP_ID, ambariIp);
        }
        return ambariIp;
    }

    private static WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Map<String, String> desiredStatuses) {
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
        while (!checkStatuses(currentStatuses, desiredStatuses) && !checkFailedStatuses(currentStatuses) && retryCount < MAX_RETRY);

        LOGGER.info("Status(es) {} for {} are in desired status(es) {}", desiredStatuses.keySet(), stackName, currentStatuses.values());
        if (currentStatuses.values().stream().anyMatch(cs -> cs.contains("FAILED")) || checkNotExpectedDelete(currentStatuses, desiredStatuses)) {
            waitResult = WaitResult.FAILED;
        }
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
            List<AutoscaleClusterHistoryResponse> autoscaleClusterHistoryResponse = historyEndpoint.getHistory(clusterId);
            for (AutoscaleClusterHistoryResponse elem : autoscaleClusterHistoryResponse) {
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

    private static int getNodeCount(StackV4Response stackResponse) {
        List<InstanceGroupV4Response> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupV4Response instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getName())) {
                nodeCount += instanceGroup.getNodeCount();
            }
        }
        return nodeCount;
    }
}