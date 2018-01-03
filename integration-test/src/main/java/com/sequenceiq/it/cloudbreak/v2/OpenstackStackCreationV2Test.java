package com.sequenceiq.it.cloudbreak.v2;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class OpenstackStackCreationV2Test extends AbstractCloudbreakIntegrationTest {
    @BeforeMethod(groups = "V2StackCreationInit")
    @Parameters({"stackName", "credentialName", "region", "availabilityZone", "imageCatalog", "imageId"})
    public void createStackRequest(@Optional("openstack-v2") String stackName, @Optional("") String credentialName, @Optional("") String region,
            @Optional("") String availabilityZone, @Optional("") String imageCatalog, @Optional("") String imageId) {
        IntegrationTestContext itContext = getItContext();

        credentialName = StringUtils.hasText(credentialName) ? credentialName : itContext.getContextParam(CloudbreakV2Constants.CREDENTIAL_NAME);
        region = StringUtils.hasText(region) ? region : itContext.getContextParam(CloudbreakV2Constants.REGION);
        availabilityZone = StringUtils.hasText(availabilityZone) ? availabilityZone : itContext.getContextParam(CloudbreakV2Constants.AVAILABILTYZONE);
        imageCatalog = StringUtils.hasText(imageCatalog) ? imageCatalog : itContext.getContextParam(CloudbreakV2Constants.IMAGECATALOG);
        imageId = StringUtils.hasText(imageId) ? imageId : itContext.getContextParam(CloudbreakV2Constants.IMAGEID);
        Map<String, InstanceGroupV2Request> instanceGroupV2RequestMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);

        Assert.assertTrue(StringUtils.hasText(credentialName), "Credential name is mandatory.");
        Assert.assertTrue(StringUtils.hasText(region), "Region is mandatory.");
        Assert.assertTrue(StringUtils.hasText(availabilityZone), "AvailabilityZone is mandatory.");
        Assert.assertNotNull(instanceGroupV2RequestMap, "InstanceGroup map is mandatory");

        StackV2Request stackV2Request = new StackV2Request();
        stackV2Request.setName(stackName);
        stackV2Request.setCredentialName(credentialName);
        stackV2Request.setRegion(region);
        stackV2Request.setAvailabilityZone(availabilityZone);
        if (StringUtils.hasText(imageCatalog)) {
            stackV2Request.setImageCatalog(imageCatalog);
        }
        if (StringUtils.hasText(imageId)) {
            stackV2Request.setImageId(imageId);
        }
        stackV2Request.setInstanceGroups(Lists.newArrayList(instanceGroupV2RequestMap.values()));

        // Default request parameters
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType("SALT");
        stackV2Request.setOrchestrator(orchestratorRequest);

        itContext.putContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, stackV2Request);
    }

    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters({"subnetCidr", "floatingPool"})
    public void networkParams(String subnetCidr, @Optional("") String floatingPool) {
        IntegrationTestContext itContext = getItContext();
        subnetCidr = StringUtils.hasText(subnetCidr) ? subnetCidr : itContext.getContextParam(CloudbreakV2Constants.SUBNET_CIDR);
        floatingPool = StringUtils.hasText(floatingPool) ? floatingPool : itContext.getContextParam(CloudbreakV2Constants.OPENSTACK_FLOATING_POOL);

        Assert.assertNotNull(subnetCidr, "Subnet cidr is mandatory.");

        StackV2Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class);
        NetworkV2Request networkRequest = new NetworkV2Request();
        networkRequest.setSubnetCIDR(subnetCidr);
        Map<String, Object> networkParameters = Maps.newHashMap();
        if (StringUtils.hasText(floatingPool)) {
            networkParameters.put("publicNetId", floatingPool);
        }
        networkRequest.setParameters(networkParameters);
        stackV2Request.setNetwork(networkRequest);
    }

    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters({"publicKeyId"})
    public void authenticationParams(@Optional("") String publicKeyId) {
        IntegrationTestContext itContext = getItContext();
        publicKeyId = StringUtils.hasText(publicKeyId) ? publicKeyId : itContext.getContextParam(CloudbreakV2Constants.SSH_PUBLICKEY_ID);

        Assert.assertNotNull(publicKeyId, "Publickey id is mandatory.");

        StackV2Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class);
        StackAuthenticationRequest stackAuthenticationRequest = new StackAuthenticationRequest();
        stackAuthenticationRequest.setPublicKeyId(publicKeyId);
        stackV2Request.setStackAuthentication(stackAuthenticationRequest);
    }

    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters({"blueprintName"})
    public void ambariParameters(@Optional("") String blueprintName) {
        IntegrationTestContext itContext = getItContext();
        blueprintName = StringUtils.hasText(blueprintName) ? blueprintName : itContext.getContextParam(CloudbreakV2Constants.SSH_PUBLICKEY_ID);

        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password is mandatory.");
        Assert.assertNotNull(blueprintName, "blueprint name is mandatory.");

        StackV2Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class);
        ClusterV2Request clusterV2Request = new ClusterV2Request();
        stackV2Request.setClusterRequest(clusterV2Request);
        AmbariV2Request ambariV2Request = new AmbariV2Request();
        clusterV2Request.setAmbariRequest(ambariV2Request);
        ambariV2Request.setBlueprintName(blueprintName);
        ambariV2Request.setUserName(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID));
        ambariV2Request.setPassword(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID));
    }

    @Test
    public void testStackCreation() throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        StackV2Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class);
        // WHEN
        String stackId = getCloudbreakClient().stackV2Endpoint().postPrivate(stackV2Request).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId, true);
        itContext.putContextParam(CloudbreakV2Constants.STACK_NAME, stackV2Request.getName());
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), stackId, desiredStatuses);
    }
}
