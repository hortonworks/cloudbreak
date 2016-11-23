package com.sequenceiq.it.cloudbreak.scaling;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ScalingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.scaling.ScalingUtil.class);

    private ScalingUtil() {
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

    public static void checkStackScaled(StackEndpoint stackEndpoint, String stackId, int expectedNodeCount) {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");
        Assert.assertEquals(expectedNodeCount, getNodeCount(stackResponse),
                "After scaling, the number of the nodes in stack differs from the expected number!");
    }

    public static void checkClusterScaled(StackEndpoint stackEndpoint, String port, String stackId, String ambariUser, String ambariPassword,
            int expectedNodeCount, IntegrationTestContext itContext) {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        Assert.assertEquals(stackResponse.getCluster().getStatus(), "AVAILABLE", "The cluster hasn't been started!");
        Assert.assertEquals(stackResponse.getStatus(), Status.AVAILABLE, "The stack hasn't been started!");

        String ambariIp = CloudbreakUtil.getAmbariIp(stackEndpoint, stackId, itContext);

        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!");

        Assert.assertEquals(expectedNodeCount, ambariClient.getClusterHosts().size(),
                "After scaling, the number of the nodes registered in ambari differs from the expected number!");
    }

    public static int getNodeCountStack(StackEndpoint stackEndpoint, String stackId) {
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));
        return getNodeCount(stackResponse);
    }

    public static int getNodeCountAmbari(StackEndpoint stackEndpoint, String port, String stackId, String ambariUser, String ambariPassword,
            IntegrationTestContext itContext) {

        String ambariIp = CloudbreakUtil.getAmbariIp(stackEndpoint, stackId, itContext);

        AmbariClient ambariClient = new AmbariClient(ambariIp, port, ambariUser, ambariPassword);
        return ambariClient.getClusterHosts().size();
    }
}
