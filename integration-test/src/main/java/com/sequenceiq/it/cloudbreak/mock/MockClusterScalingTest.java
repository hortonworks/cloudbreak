package com.sequenceiq.it.cloudbreak.mock;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
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
    public void testScaling(@Optional("slave_1") String instanceGroup, @Optional("1") int scalingAdjustment, @Optional("9443") int mockPort) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        int stackIntId = Integer.parseInt(stackId);
        // WHEN
        if (scalingAdjustment < 0) {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setHostGroup(instanceGroup);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("DownscaleCluster", getCloudbreakClient().clusterEndpoint().put((long) stackIntId, updateClusterJson));
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, "AVAILABLE");

            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setWithClusterEvent(false);
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("DownscaleStack", getCloudbreakClient().stackV1Endpoint().put((long) stackIntId, updateStackJson));
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        } else {
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setWithClusterEvent(false);
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("UpscaleStack", getCloudbreakClient().stackV1Endpoint().put((long) stackIntId, updateStackJson));
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");

            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setHostGroup(instanceGroup);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("UpscaleCluster", getCloudbreakClient().clusterEndpoint().put((long) stackIntId, updateClusterJson));
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        }
        // THEN
        CloudbreakUtil.checkClusterAvailability(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV1Endpoint(),
                "8080", stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
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
