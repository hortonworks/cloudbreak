package com.sequenceiq.it.cloudbreak.v2.mock;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.StackScalingV2Test;

public class MockScalingTest extends StackScalingV2Test {
    @BeforeMethod
    public void setContextParameters() {
        super.setContextParameters();
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class),
                "StackCreationRequest is mandatory.");
    }

    @BeforeClass
    @Parameters({"mockPort", "sshPort", "desiredCount", "hostGroup"})
    public void configMockServer(@Optional("9443") int mockPort, @Optional("2020") int sshPort, int desiredCount, String hostGroup) {
        IntegrationTestContext itContext = getItContext();
        String clusterName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);
        StackResponse response = getCloudbreakClient().stackV2Endpoint().getPrivate(clusterName, null);
        java.util.Optional<InstanceGroupResponse> igg = response.getInstanceGroups().stream().filter(ig -> ig.getGroup().equals(hostGroup)).findFirst();
        Map<String, CloudVmInstanceStatus> instanceMap = itContext.getContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, Map.class);
        ScalingMock scalingMock = (ScalingMock) applicationContext.getBean(ScalingMock.NAME, mockPort, sshPort, instanceMap);
        scalingMock.addSPIEndpoints();
        scalingMock.addMockEndpoints();
        scalingMock.addAmbariMappings(clusterName);
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, scalingMock);
        igg.ifPresent(ig -> {
            int scalingAdjustment = desiredCount - ig.getNodeCount();
            if (scalingAdjustment > 0) {
                scalingMock.addInstance(scalingAdjustment);
            }
        });
    }

    @Test
    @Parameters({"hostGroup", "desiredCount", "checkAmbari"})
    public void testStackScaling(String hostGroup, int desiredCount, @Optional("false") boolean checkAmbari) throws Exception {
        // GIVEN
        // WHEN
        super.testStackScaling(hostGroup, desiredCount, checkAmbari);
        // THEN
        ScalingMock scalingMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, ScalingMock.class);
        String clusterName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        String stackId = getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID);
        StackV2Request stackV2Request = getItContext().getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class);
        boolean securityEnabled = stackV2Request.getCluster().getAmbari().getEnableSecurity();
        scalingMock.verifyV2Calls(clusterName, ScalingUtil.getNodeCountStack(getCloudbreakClient().stackV2Endpoint(), stackId), securityEnabled);
    }

    @AfterClass
    public void breakDown() {
        ScalingMock scalingMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, ScalingMock.class);
        scalingMock.stop();
    }
}
