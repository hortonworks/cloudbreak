package com.sequenceiq.it.cloudbreak.scaling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.services.ServiceAndHostService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ScalingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.scaling.ScalingUtil.class);

    private ScalingUtil() {
    }

    public static void checkStackScaled(StackV4Endpoint stackV4Endpoint, Long workspaceId, String stackName, int expectedNodeCount) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkStackScaled(expectedNodeCount, stackResponse);
    }

    public static void checkStackScaled(StackV4Endpoint stackV4Endpoint, Long workspaceId, String stackName, String instanceGroup, int expectedNodeCount) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkStackScaled(instanceGroup, expectedNodeCount, stackResponse);
    }

    private static void checkStackScaled(Integer expectedNodeCount, StackV4Response stackResponse) {
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
        Assert.assertEquals(expectedNodeCount, getNodeCount(stackResponse),
                "After scaling, the number of the nodes in stack differs from the expected number!");
    }

    private static void checkStackScaled(String instanceGroup, Integer expectedNodeCount, StackV4Response stackResponse) {
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
        Assert.assertEquals(expectedNodeCount, getNodeCount(stackResponse, instanceGroup),
                "After scaling, the number of the nodes in stack differs from the expected number!");
    }

    public static void checkClusterScaled(StackV4Endpoint stackV4Endpoint, String port, Long workspaceId, String stackName, String ambariUser, String ambariPassword,
            int expectedNodeCount, IntegrationTestContext itContext) {
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());
        checkClusterScaled(CloudbreakUtil.getAmbariIp(stackResponse, itContext), port, ambariUser, ambariPassword, expectedNodeCount, stackResponse);
    }

    private static void checkClusterScaled(String ambariIp, String port, String ambariUser, String ambariPassword, int expectedNodeCount,
            StackV4Response stackResponse) {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE, "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");

        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");

        Assert.assertEquals(expectedNodeCount, ambariClient.getClusterHosts().size(),
                "After scaling, the number of the nodes registered in ambari differs from the expected number!");
    }

    public static int getNodeCountStack(StackV4Endpoint stackV1Endpoint, Long workspaceId, String stackName) {
        StackV4Response stackResponse = stackV1Endpoint.get(workspaceId, stackName, new HashSet<>());
        return getNodeCount(stackResponse);
    }

    public static int getNodeCountAmbari(StackV4Endpoint stackV4Endpoint, String port, Long workspaceId, String stackId, String ambariUser, String ambariPassword,
            IntegrationTestContext itContext) {

        String ambariIp = CloudbreakUtil.getAmbariIp(stackV4Endpoint, workspaceId, stackId, itContext);

        ServiceAndHostService ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        return ambariClient.getClusterHosts().size();
    }

    public static Map<String, Integer> getNodeCountByHostgroup(StackV4Response stackResponse) {
        Map<String, Integer> instanceCount = new HashMap<>();

        List<InstanceGroupV4Response> instanceGroups = stackResponse.getInstanceGroups();

        for (InstanceGroupV4Response instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getName())) {
                instanceCount.put(instanceGroup.getName(), instanceGroup.getNodeCount());
            }
        }
        return instanceCount;
    }

    public static void putInstanceCountToContext(IntegrationTestContext itContext, Long workspaceId, String stackName) {
        Collection<Map<String, Integer>> tmpInstanceCount = new ArrayList<>();
        StackV4Endpoint stackV4Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV4Endpoint();
        StackV4Response stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());

        if (itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class) != null) {
            tmpInstanceCount = itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class);
            tmpInstanceCount.add(getNodeCountByHostgroup(stackResponse));
        } else {
            tmpInstanceCount.add(getNodeCountByHostgroup(stackResponse));
        }
        itContext.putContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, tmpInstanceCount);
    }

    private static Integer getNodeCount(StackV4Response stackResponse) {
        List<InstanceGroupV4Response> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupV4Response instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getName())) {
                nodeCount += instanceGroup.getNodeCount();
            }
        }
        return nodeCount;
    }

    private static Integer getNodeCount(StackV4Response stackResponse, String instanceGroup) {
        List<InstanceGroupV4Response> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupV4Response ig : instanceGroups) {
            if (ig.getName().equals(instanceGroup)) {
                nodeCount = ig.getNodeCount();
                break;
            }
        }
        return nodeCount;
    }

}
