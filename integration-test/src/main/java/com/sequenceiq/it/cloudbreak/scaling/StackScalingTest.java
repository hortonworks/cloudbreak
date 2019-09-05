package com.sequenceiq.it.cloudbreak.scaling;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class StackScalingTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingTest.class);

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testStackScaling(@Optional("slave_1") String instanceGroup, int scalingAdjustment) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        StackV3Endpoint stackV3Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV3Endpoint();
        int expectedNodeCount = ScalingUtil.getNodeCountStack(stackV3Endpoint, workspaceId, stackName) + scalingAdjustment;
        // WHEN
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        HostGroupAdjustmentJson instanceGroupAdjustmentJson = new HostGroupAdjustmentJson();
        instanceGroupAdjustmentJson.setHostGroup(instanceGroup);
        instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        instanceGroupAdjustmentJson.setWithStackUpdate(false);
        updateStackJson.setHostGroupAdjustment(instanceGroupAdjustmentJson);
        CloudbreakUtil.checkResponse("ScalingStack", getCloudbreakClient().stackV3Endpoint().put(workspaceId, stackName, updateStackJson));
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackV3Endpoint, workspaceId, stackName, expectedNodeCount);
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());

        itContext.putContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, ScalingUtil.getNodeCountByHostgroup(stackResponse));
    }
}