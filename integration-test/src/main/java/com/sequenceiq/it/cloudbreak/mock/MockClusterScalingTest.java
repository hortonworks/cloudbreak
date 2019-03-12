package com.sequenceiq.it.cloudbreak.mock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.mock.ScalingMock;

public class MockClusterScalingTest extends AbstractCloudbreakIntegrationTest {
    private static final String CLUSTER_NAME = "ambari_cluster";

    @BeforeClass
    @Parameters({"mockPort", "sshPort", "scalingAdjustment", "instanceGroup"})
    public void configMockServer(@Optional("9443") int mockPort, @Optional("2020") int sshPort, @Optional("1") int scalingAdjustment, String instanceGroup) {
        IntegrationTestContext itContext = getItContext();
        Map<String, CloudVmMetaDataStatus> instanceMap = itContext.getContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, Map.class);
        ScalingMock scalingMock = (ScalingMock) applicationContext.getBean(ScalingMock.NAME, mockPort, sshPort, instanceMap);
        scalingMock.addSPIEndpoints();
        scalingMock.addMockEndpoints();
        scalingMock.addAmbariMappings(CLUSTER_NAME);
        if (scalingAdjustment > 0) {
            scalingMock.addInstance(scalingAdjustment);
        }
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, scalingMock);
    }

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @SuppressWarnings("Duplicates")
    @Test
    @Parameters({"instanceGroup", "scalingAdjustment", "mockPort"})
    public void testScaling(@Optional("slave_1") String instanceGroup, @Optional("1") int scalingAdjustment, @Optional("9443") int mockPort)
            throws IOException, URISyntaxException {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long stackId = Long.valueOf(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID));
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        // WHEN
        if (scalingAdjustment < 0) {
//            UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
            hostGroupAdjustmentJson.setHostGroup(instanceGroup);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
//            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);

            var stackScaleRequest = new StackScaleV4Request();
            stackScaleRequest.setGroup(hostGroupAdjustmentJson.getHostGroup());
            stackScaleRequest.setDesiredCount(scalingAdjustment);
            stackScaleRequest.setStackId(stackId);

            getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, stackScaleRequest);
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");

//            UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
//            updateStackJson.setWithClusterEvent(false);
//            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
//            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
//            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
//            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);

            stackScaleRequest.setGroup(hostGroupAdjustmentJson.getHostGroup());
            stackScaleRequest.setDesiredCount(scalingAdjustment);
            stackScaleRequest.setStackId(stackId);

            getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, stackScaleRequest);
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        } else {
//            UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
//            updateStackJson.setWithClusterEvent(false);
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
//            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);

            var stackScaleRequest3 = new StackScaleV4Request();
            stackScaleRequest3.setGroup(instanceGroupAdjustmentJson.getInstanceGroup());
            stackScaleRequest3.setDesiredCount(scalingAdjustment);
            stackScaleRequest3.setStackId(stackId);

            getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, stackScaleRequest3);
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");

//            UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
//            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
//            hostGroupAdjustmentJson.setHostGroup(instanceGroup);
//            hostGroupAdjustmentJson.setWithStackUpdate(false);
//            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
//            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);

            stackScaleRequest3.setGroup(instanceGroupAdjustmentJson.getInstanceGroup());
            stackScaleRequest3.setDesiredCount(scalingAdjustment);
            stackScaleRequest3.setStackId(stackId);
            getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, stackScaleRequest3);
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        }
        // THEN
        CloudbreakUtil.checkClusterAvailability(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV4Endpoint(),
                "8080", workspaceId, stackName, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), false);

        ScalingMock scalingMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, ScalingMock.class);
        scalingMock.verifyV1Calls(CLUSTER_NAME, scalingAdjustment);
    }

    @AfterClass
    public void breakDown() {
        ScalingMock scalingMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, ScalingMock.class);
        scalingMock.stop();
    }
}
