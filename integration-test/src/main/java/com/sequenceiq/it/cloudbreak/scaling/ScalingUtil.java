package com.sequenceiq.it.cloudbreak.scaling;

import java.io.IOException;
import java.net.URISyntaxException;
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
import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
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

    private static int getNodeCount(StackResponse stackResponse, String instanceGroup) {
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();
        int nodeCount = 0;
        for (InstanceGroupResponse ig : instanceGroups) {
            if (ig.getGroup().equals(instanceGroup)) {
                nodeCount = ig.getNodeCount();
                break;
            }
        }
        return nodeCount;
    }

    public static void checkStackScaled(StackV3Endpoint stackV3Endpoint, Long workspaceId, String stackName, int expectedNodeCount) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());
        checkStackScaled(expectedNodeCount, stackResponse);
    }

    public static void checkStackScaled(StackV3Endpoint stackV3Endpoint, Long workspaceId, String stackName, String instanceGroup, int expectedNodeCount) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());
        checkStackScaled(instanceGroup, expectedNodeCount, stackResponse);
    }

    private static void checkStackScaled(int expectedNodeCount, StackResponse stackResponse) {
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
        Assert.assertEquals(expectedNodeCount, getNodeCount(stackResponse),
                "After scaling, the number of the nodes in stack differs from the expected number!");
    }

    private static void checkStackScaled(String instanceGroup, int expectedNodeCount, StackResponse stackResponse) {
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
        Assert.assertEquals(expectedNodeCount, getNodeCount(stackResponse, instanceGroup),
                "After scaling, the number of the nodes in stack differs from the expected number!");
    }

    public static void checkClusterScaled(StackV3Endpoint stackV3Endpoint, String port, Long workspaceId, String stackName,
            String ambariUser, String ambariPassword,
            int expectedNodeCount, IntegrationTestContext itContext) throws IOException, URISyntaxException {
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());
        checkClusterScaled(CloudbreakUtil.getAmbariIp(stackResponse, itContext), port, ambariUser, ambariPassword, expectedNodeCount, stackResponse);
    }

    private static void checkClusterScaled(String ambariIp, String port, String ambariUser, String ambariPassword, int expectedNodeCount,
            StackResponse stackResponse) throws IOException, URISyntaxException {
        Assert.assertEquals(stackResponse.getCluster().getStatus(), Status.AVAILABLE, "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");

        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");

        Assert.assertEquals(expectedNodeCount, ambariClient.getClusterHosts().size(),
                "After scaling, the number of the nodes registered in ambari differs from the expected number!");
    }

    public static int getNodeCountStack(StackV3Endpoint stackV3Endpoint, Long workspaceId, String stackName) {
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());
        return getNodeCount(stackResponse);
    }

    public static int getNodeCountAmbari(StackV3Endpoint stackV3Endpoint, String port, Long workspaceId, String stackName,
            String ambariUser, String ambariPassword, IntegrationTestContext itContext) {

        String ambariIp = CloudbreakUtil.getAmbariIp(stackV3Endpoint, workspaceId, stackName, itContext);

        ServiceAndHostService ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        return ambariClient.getClusterHosts().size();
    }

    public static Map<String, Integer> getNodeCountByHostgroup(StackResponse stackResponse) {
        Map<String, Integer> instanceCount = new HashMap<>();

        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();

        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (!"cbgateway".equals(instanceGroup.getGroup())) {
                instanceCount.put(instanceGroup.getGroup(), instanceGroup.getNodeCount());
            }
        }
        return instanceCount;
    }

    public static void putInstanceCountToContext(IntegrationTestContext itContext, Long workspaceId, String stackName) {
        Collection<Map<String, Integer>> tmpInstanceCount = new ArrayList<>();
        StackV3Endpoint stackV3Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV3Endpoint();
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());

        if (itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class) != null) {
            tmpInstanceCount = itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class);
            tmpInstanceCount.add(getNodeCountByHostgroup(stackResponse));
        } else {
            tmpInstanceCount.add(getNodeCountByHostgroup(stackResponse));
        }
        itContext.putContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, tmpInstanceCount);
    }
}
