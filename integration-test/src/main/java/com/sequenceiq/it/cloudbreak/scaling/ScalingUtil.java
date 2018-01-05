package com.sequenceiq.it.cloudbreak.scaling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ScalingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.scaling.ScalingUtil.class);

    private ScalingUtil() {
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

    public static void checkStackScaled(StackV1Endpoint stackV1Endpoint, String stackId, int expectedNodeCount) {
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        checkStackScaled(expectedNodeCount, stackResponse);
    }

    public static void checkStackScaled(StackV2Endpoint stackV2Endpoint, String stackId, int expectedNodeCount) {
        StackResponse stackResponse = stackV2Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        checkStackScaled(expectedNodeCount, stackResponse);
    }

    private static void checkStackScaled(int expectedNodeCount, StackResponse stackResponse) {
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
        Assert.assertEquals(expectedNodeCount, getNodeCount(stackResponse),
                "After scaling, the number of the nodes in stack differs from the expected number!");
    }

    public static void checkClusterScaled(StackV1Endpoint stackV1Endpoint, String port, String stackId, String ambariUser, String ambariPassword,
            int expectedNodeCount, IntegrationTestContext itContext) {
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        checkClusterScaled(CloudbreakUtil.getAmbariIp(stackResponse, itContext), port, ambariUser, ambariPassword, expectedNodeCount, stackResponse);
    }

    public static void checkClusterScaled(StackV2Endpoint stackV2Endpoint, String port, String stackId, String ambariUser, String ambariPassword,
            int expectedNodeCount, IntegrationTestContext itContext) {
        StackResponse stackResponse = stackV2Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        checkClusterScaled(CloudbreakUtil.getAmbariIp(stackResponse, itContext), port, ambariUser, ambariPassword, expectedNodeCount, stackResponse);
    }

    private static void checkClusterScaled(String ambariIp, String port, String ambariUser, String ambariPassword, int expectedNodeCount,
            StackResponse stackResponse) {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE, "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");

        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");

        Assert.assertEquals(expectedNodeCount, ambariClient.getClusterHosts().size(),
                "After scaling, the number of the nodes registered in ambari differs from the expected number!");
    }

    public static int getNodeCountStack(StackV1Endpoint stackV1Endpoint, String stackId) {
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        return getNodeCount(stackResponse);
    }

    public static int getNodeCountStack(StackV2Endpoint stackV2Endpoint, String stackId) {
        StackResponse stackResponse = stackV2Endpoint.get(Long.valueOf(stackId), new HashSet<>());
        return getNodeCount(stackResponse);
    }

    public static int getNodeCountAmbari(StackV1Endpoint stackV1Endpoint, String port, String stackId, String ambariUser, String ambariPassword,
            IntegrationTestContext itContext) {

        String ambariIp = CloudbreakUtil.getAmbariIp(stackV1Endpoint, stackId, itContext);

        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        return ambariClient.getClusterHosts().size();
    }

    public static Map getNodeCountByHostgroup(StackResponse stackResponse) {
        Map<String, Integer> instanceCount = new HashMap<>();

        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();

        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getGroup())) {
                instanceCount.put(instanceGroup.getGroup(), instanceGroup.getNodeCount());
            }
        }
        return instanceCount;
    }

    public static void putInstanceCountToContext(IntegrationTestContext itContext, String stackId) {
        List<Map<String, Integer>> tmpInstanceCount = new ArrayList<>();
        StackV1Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV1Endpoint();
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());

        if (itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class) != null) {
            tmpInstanceCount = itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class);
            tmpInstanceCount.add(getNodeCountByHostgroup(stackResponse));
        } else {
            tmpInstanceCount.add(getNodeCountByHostgroup(stackResponse));
        }
        itContext.putContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, tmpInstanceCount);
    }
}
