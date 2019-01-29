package com.sequenceiq.it.cloudbreak.v2.mock;

import java.util.HashSet;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
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
        Assert.assertNotNull(itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV4Request.class),
                "StackCreationRequest is mandatory.");
    }

    @BeforeClass
    @Parameters({"mockPort", "sshPort", "desiredCount", "hostGroup"})
    public void configMockServer(@Optional("9443") int mockPort, @Optional("2020") int sshPort, int desiredCount, String hostGroup) {
        IntegrationTestContext itContext = getItContext();
        String clusterName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        StackV4Response response = getCloudbreakClient().stackV4Endpoint().get(workspaceId, clusterName, new HashSet<>());
        java.util.Optional<InstanceGroupV4Response> igg = response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(hostGroup)).findFirst();
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
        String stackName = getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        var stackV2Request = getItContext().getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV4Request.class);
        scalingMock.verifyV2Calls(stackV2Request.getCluster(), ScalingUtil.getNodeCountStack(getCloudbreakClient().stackV4Endpoint(), workspaceId, stackName));
    }

    @AfterClass
    public void breakDown() {
        ScalingMock scalingMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, ScalingMock.class);
        scalingMock.stop();
    }
}
