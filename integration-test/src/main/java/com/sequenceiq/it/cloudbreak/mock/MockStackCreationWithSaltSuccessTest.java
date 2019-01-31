package com.sequenceiq.it.cloudbreak.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.InstanceGroup;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.mock.StackCreationMock;
import com.sequenceiq.it.util.ResourceUtil;

public class MockStackCreationWithSaltSuccessTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.publicKeyFile}")
    private String defaultPublicKeyFile;

    @BeforeClass
    @Parameters({"stackName", "mockPort", "sshPort"})
    public void configMockServer(String stackName, @Optional("9443") int mockPort, @Optional("2020") int sshPort) {
        IntegrationTestContext itContext = getItContext();
        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        int numberOfServers = 0;
        for (InstanceGroup ig : instanceGroups) {
            numberOfServers += ig.getNodeCount();
        }
        StackCreationMock stackCreationMock = (StackCreationMock) applicationContext.getBean(
                StackCreationMock.NAME, mockPort, sshPort, numberOfServers);
        stackCreationMock.addSPIEndpoints();
        stackCreationMock.mockImageCatalogResponse(itContext);
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, stackCreationMock);
        itContext.putContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, stackCreationMock.getInstanceMap());
    }

    @BeforeMethod
    public void setContextParams() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class), "Template id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "network id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.");
    }

    @Test
    @Parameters({"stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "orchestrator",
            "mockPort", "sshPort", "publicKeyFile"})
    public void testStackCreation(@Optional("testing1") String stackName, @Optional("europe-west1") String region,
            @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold, @Optional("EXACT") String adjustmentType,
            @Optional("") String variant, @Optional String availabilityZone, @Optional String persistentStorage, @Optional("SALT") String orchestrator,
            @Optional("9443") int mockPort, @Optional("2020") int sshPort, @Optional("") String publicKeyFile)
            throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        List<InstanceGroupV4Request> igMap = new ArrayList<>();

        for (InstanceGroup ig : instanceGroups) {
            InstanceGroupV4Request instanceGroupRequest = new InstanceGroupV4Request();
            instanceGroupRequest.setName(ig.getName());
            instanceGroupRequest.setNodeCount(ig.getNodeCount());
            // FIXME: should figure out how to fill the template
//            instanceGroupRequest.setTemplateId(Long.valueOf(ig.getTemplateId()));
            instanceGroupRequest.setType(InstanceGroupType.valueOf(ig.getType()));
            igMap.add(instanceGroupRequest);
        }

        String credentialName = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        var stackRequest = new StackV4Request();

        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        var stackAuthenticationRequest = new StackAuthenticationV4Request();
        var environment = new EnvironmentV4Request();
        stackAuthenticationRequest.setPublicKey(publicKey);
        stackRequest.setAuthentication(stackAuthenticationRequest);
        environment.setCredentialName(credentialName);
        environment.setRegions(Set.of(region));
        stackRequest.setName(stackName);

        // FIXME: should figure out how on earth can we obtain a valid network request and where to set the availability zone
        // stackRequest.setNetworkId(Long.valueOf(networkId));
        // stackRequest.setAvailabilityZone(availabilityZone);
        stackRequest.setCloudPlatform(CloudPlatform.valueOf(variant.toUpperCase()));

        stackRequest.setInstanceGroups(igMap);

        // THEN
        Assert.assertNotNull(stackName);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackName);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackName);
    }

    @AfterClass
    public void breakDown() {
        StackCreationMock stackCreationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, StackCreationMock.class);
        stackCreationMock.stop();
    }
}
