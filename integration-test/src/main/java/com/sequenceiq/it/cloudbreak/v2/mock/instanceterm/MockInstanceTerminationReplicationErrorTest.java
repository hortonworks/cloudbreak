package com.sequenceiq.it.cloudbreak.v2.mock.instanceterm;

import static java.util.Collections.singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.mock.MockServer;

public class MockInstanceTerminationReplicationErrorTest extends AbstractCloudbreakIntegrationTest {

    @BeforeClass
    @Parameters({"mockPort", "sshPort"})
    public void configMockServer(@Optional("9443") int mockPort, @Optional("2020") int sshPort) {
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV2Request> instanceGroupV2RequestMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        String stackName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);
        int numberOfServers = 0;
        for (InstanceGroupV2Request igr : instanceGroupV2RequestMap.values()) {
            numberOfServers += igr.getNodeCount();
        }
        InstanceTerminationReplicationErrorMock instanceTerminationMock = (InstanceTerminationReplicationErrorMock)
                applicationContext.getBean(InstanceTerminationReplicationErrorMock.NAME, mockPort, sshPort, numberOfServers);
        instanceTerminationMock.addAmbariMappings(stackName);
        instanceTerminationMock.addMockEndpoints();
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, instanceTerminationMock);
        itContext.putContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, instanceTerminationMock.getInstanceMap());
    }

    @Test
    public void testInstanceTermination() {
        // GIVEN
        // WHEN
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        StackResponse stackResponse = getStackResponse(workspaceId, stackName);
        // THEN
        String hostGroupName = "worker";

        String instanceId = getInstanceId(stackResponse, hostGroupName);

        getCloudbreakClient().stackV3Endpoint().deleteInstance(workspaceId, stackResponse.getName(), instanceId, false);
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        String reason = CloudbreakUtil.getFailedStatusReason(getCloudbreakClient(), workspaceId, stackName, desiredStatuses, singleton(WaitResult.SUCCESSFUL));
        Assert.assertEquals(reason, "Node(s) could not be removed from the cluster: There is not enough node to downscale. "
                + "Check the replication factor and the ApplicationMaster occupation.");
    }

    @Test
    public void testInstanceTerminationForced() {
        // GIVEN
        // WHEN
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        StackResponse stackResponse = getStackResponse(workspaceId, stackName);
        // THEN
        String hostGroupName = "worker";

        String instanceId = getInstanceId(stackResponse, hostGroupName);
        int before = getInstanceMetaData(stackResponse, hostGroupName).size();

        getCloudbreakClient().stackV3Endpoint().deleteInstance(workspaceId, stackName, instanceId, true);
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
        int after = getInstanceMetaData(getStackResponse(workspaceId, stackName), hostGroupName).size();

        Assert.assertEquals(after, before - 1);
    }

    protected StackResponse getStackResponse(Long workspaceId, String stackName) {
        return getCloudbreakClient().stackV3Endpoint().getByNameInWorkspace(workspaceId, stackName, Collections.emptySet());
    }

    protected String getInstanceId(StackResponse stackResponse, String hostGroupName) {
        Set<InstanceMetaDataJson> metadata = getInstanceMetaData(stackResponse, hostGroupName);
        return metadata
                .stream()
                .findFirst()
                .get()
                .getInstanceId();
    }

    private Set<InstanceMetaDataJson> getInstanceMetaData(StackResponse stackResponse, String hostGroupName) {
        return stackResponse.getInstanceGroups()
                .stream().filter(im -> im.getGroup().equals(hostGroupName))
                .findFirst()
                .get()
                .getMetadata();
    }

    @AfterClass
    public void breakDown() {
        MockServer instanceTerminationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, InstanceTerminationReplicationErrorMock.class);
        instanceTerminationMock.stop();
    }

}
