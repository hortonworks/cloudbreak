package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.mock.restito.consul.ConsulMembersStub;
import com.sequenceiq.it.mock.restito.docker.DockerContainersGetStub;
import com.sequenceiq.it.mock.restito.docker.DockerContainersStartPostStub;
import com.sequenceiq.it.mock.restito.docker.DockerInfoGetStub;
import com.sequenceiq.it.mock.restito.docker.SwarmInfoStub;
import com.xebialabs.restito.server.StubServer;

public class MockStackCreationSuccessTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParams() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class), "Template id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "Network id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.");
    }

    @Test
    @Parameters({"stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "mockPort"})
    public void testStackCreation(@Optional("testing1") String stackName, @Optional("europe-west1") String region,
            @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold, @Optional("EXACT") String adjustmentType,
            @Optional("") String variant, @Optional() String availabilityZone, @Optional() String persistentStorage, @Optional("443") int mockPort) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        List<InstanceGroupJson> igMap = new ArrayList<>();
        for (InstanceGroup ig : instanceGroups) {
            InstanceGroupJson instanceGroupJson = new InstanceGroupJson();
            instanceGroupJson.setGroup(ig.getName());
            instanceGroupJson.setNodeCount(ig.getNodeCount());
            instanceGroupJson.setTemplateId(Long.valueOf(ig.getTemplateId()));
            instanceGroupJson.setType(InstanceGroupType.valueOf(ig.getType()));
            igMap.add(instanceGroupJson);
        }
        String credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID);
        String networkId = itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID);
        String securityGroupId = itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setName(stackName);
        stackRequest.setCredentialId(Long.valueOf(credentialId));
        stackRequest.setRegion(region);
        stackRequest.setOnFailureAction(OnFailureAction.valueOf(onFailureAction));
        FailurePolicyJson failurePolicyJson = new FailurePolicyJson();
        failurePolicyJson.setAdjustmentType(AdjustmentType.valueOf(adjustmentType));
        failurePolicyJson.setThreshold(threshold);
        stackRequest.setFailurePolicy(failurePolicyJson);
        stackRequest.setNetworkId(Long.valueOf(networkId));
        stackRequest.setSecurityGroupId(Long.valueOf(securityGroupId));
        stackRequest.setPlatformVariant(variant);
        stackRequest.setAvailabilityZone(availabilityZone);
        stackRequest.setInstanceGroups(igMap);

        Map<String, String> map = new HashMap<>();
        if (persistentStorage != null && !persistentStorage.isEmpty()) {
            map.put("persistentStorage", persistentStorage);
        }
        stackRequest.setParameters(map);

        int numberOfServers = getNumberOfServers(instanceGroups);

        StubServer stubServer = startMockServer(mockPort);
        addStubs(stubServer, numberOfServers);

        // WHEN
        String stackId = getCloudbreakClient().stackEndpoint().postPrivate(stackRequest).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId);

        stubServer.stop();
    }

    private void addStubs(StubServer stubServer, int numberOfServers) {
        stubServer.addStub(new DockerInfoGetStub());
        stubServer.addStub(new DockerContainersGetStub());
        stubServer.addStub(new DockerContainersStartPostStub());
        stubServer.addStub(new SwarmInfoStub(numberOfServers));
        stubServer.addStub(new ConsulMembersStub(numberOfServers));
    }

    private int getNumberOfServers(List<InstanceGroup> instanceGroups) {
        int numberOfServers = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            numberOfServers += instanceGroup.getNodeCount();
        }
        return numberOfServers;
    }

    private StubServer startMockServer(int mockPort) {
        return new StubServer(mockPort).secured().run();
    }
}
