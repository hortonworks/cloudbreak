package com.sequenceiq.it.cloudbreak.newway.v3;

import static java.lang.Boolean.FALSE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class CloudbreakV3Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakV3Util.class);

    private static final int MAX_RETRY = 360;

    private static long pollingInterval = 10000L;

    private CloudbreakV3Util() {
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

    public static Map<String, String> waitAndCheckStackStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatus) {
        return waitAndCheckStatuses(cloudbreakClient, workspaceId, stackName, desiredStatus);
    }

    public static Map<String, String> waitAndCheckClusterStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatus) {
        return waitAndCheckStatuses(cloudbreakClient, workspaceId, stackName, desiredStatus);
    }

    public static WaitResult waitForStackStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatus) {
        return waitForStatuses(cloudbreakClient, workspaceId, stackName, desiredStatus);
    }

    public static WaitResult waitForClusterStatus(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatus) {
        return waitForStatuses(cloudbreakClient, workspaceId, stackName, desiredStatus);
    }

    public static String getFailedStatusReason(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatuses,
            Collection<WaitResult> desiredWaitResult) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, workspaceId, stackName, desiredStatuses);
            if (!desiredWaitResult.contains(waitResult)) {
                Assert.fail("Expected status is failed, actual: " + waitResult);
            } else {
                StackV4Endpoint stackV4Endpoint = cloudbreakClient.stackV4Endpoint();
                return stackV4Endpoint.getStatusByName(workspaceId, stackName).getStatusReason();
            }
        }
        return "";
    }

    public static Map<String, String> waitAndCheckStatuses(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatuses) {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, workspaceId, stackName, desiredStatuses);
            if (waitResult == WaitResult.FAILED) {
                StackStatusV4Response statusByNameInWorkspace = cloudbreakClient.stackV4Endpoint().getStatusByName(workspaceId, stackName);
                if (statusByNameInWorkspace != null) {
                    Assert.fail(String.format("The stack has failed: %s", statusByNameInWorkspace.getStatusReason()));
                }
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
        }
        return ret;
    }

    public static void waitAndExpectClusterFailure(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatuses,
            String keyword) {
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitForStatuses(cloudbreakClient, workspaceId, stackName, desiredStatuses);
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened");
            }
            StackV4Endpoint stackV3Endpoint = cloudbreakClient.stackV4Endpoint();
            Assert.assertTrue(stackV3Endpoint.getStatusByName(workspaceId, stackName).getClusterStatusReason().contains(keyword));
        }
    }

    public static void checkClusterAvailability(StackV4Endpoint stackV4Endpoint, String port, Long workspaceId, String stackName, String ambariUser,
            String ambariPassowrd, boolean checkAmbari) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkClusterAvailability(stackResponse, port, ambariUser, ambariPassowrd, checkAmbari);
    }

    public static void checkClusterAvailabilityThroughGateway(StackV4Endpoint stackV3Endpoint, Long workspaceId, String stackName) {
        StackV4Response stackResponse = stackV3Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkStackStatusForClusterAvailability(stackResponse);

        String ambariServerUrl = stackResponse.getCluster().getAmbari().getServerIp();
        Assert.assertNotNull(ambariServerUrl, "The Ambari URL is not available!");
        Response response = RestClientUtil.get().target(ambariServerUrl).request().get();
        Assert.assertEquals(HttpStatus.OK.value(), response.getStatus(), "Ambari is not available!");
    }

    private static void checkClusterAvailability(StackV4Response stackResponse, String port, String ambariUser, String ambariPassowrd,
            boolean checkAmbari) {
        checkStackStatusForClusterAvailability(stackResponse);

        String ambariIp = stackResponse.getCluster().getAmbari().getServerIp();
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

    public static boolean isAmbariRunning(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
            return "RUNNING".equals(ambariHealth);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static WaitResult waitForStatuses(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, Status desiredStatus) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Status currentStatus = null;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for status(es) {}, stack id: {}, current status(es) {} ...", desiredStatus, stackName, null);

            sleep();
            try {
                StackStatusV4Response statusResult = cloudbreakClient.stackV4Endpoint().getStatusByName(workspaceId, stackName);
                if (statusResult.getStatus() != null) {
                    currentStatus = statusResult.getStatus();
                } else {
                    currentStatus = Status.DELETE_COMPLETED;
                }
            } catch (ForbiddenException e) {
                if (desiredStatus == Status.DELETE_COMPLETED) {
                    currentStatus = Status.DELETE_COMPLETED;
                }
            } catch (RuntimeException ignore) {
                continue;
            }

            retryCount++;
        }
        while (!checkStatus(currentStatus, desiredStatus) && !checkFailedStatuses(currentStatus) && retryCount < MAX_RETRY);

        LOGGER.info("Status(es) {} for {} are in desired status(es) {}", desiredStatus, stackName, currentStatus);
        if (currentStatus.name().contains("FAILED") || checkNotExpectedDelete(currentStatus, desiredStatus)) {
            waitResult = WaitResult.FAILED;
        }
        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT;
        }
        return waitResult;
    }

    public static WaitResult waitForEvent(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName, String eventType,
            String eventMessage, long sinceTimeStamp) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Boolean exitCriteria = FALSE;

        int retryCount = 0;
        do {
            LOGGER.info("Waiting for event type {} and event message contains {} ...", eventType, eventMessage);
            sleep();
            var events = cloudbreakClient.eventV3Endpoint().list(workspaceId, sinceTimeStamp).getResponses();
            for (CloudbreakEventV4Response event : events) {
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

    private static boolean checkStatus(Status currentStatus, Status desiredStatus) {
        return currentStatus == desiredStatus;
    }

    private static boolean checkFailedStatuses(Status currentStatus) {
        return currentStatus == Status.DELETE_COMPLETED || Status.CREATE_FAILED == currentStatus;
    }

    private static boolean checkNotExpectedDelete(Status currentStatus, Status desiredStatus) {
        return Status.DELETE_COMPLETED != desiredStatus && Status.DELETE_COMPLETED == currentStatus;
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
            for (InstanceMetaDataV4Response metadatum : instanceGroup.getMetadata()) {
                if (!"cbgateway".equals(metadatum.getInstanceGroup())) {
                    nodeCount += instanceGroup.getNodeCount();
                }
            }
        }
        return nodeCount;
    }
}