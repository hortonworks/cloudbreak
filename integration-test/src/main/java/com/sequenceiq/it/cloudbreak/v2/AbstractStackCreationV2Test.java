package com.sequenceiq.it.cloudbreak.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class AbstractStackCreationV2Test extends AbstractCloudbreakIntegrationTest {
    @BeforeMethod(groups = "V2StackCreationInit")
    @Parameters({"stackName", "credentialName", "region", "availabilityZone", "imageCatalog", "imageId"})
    public void createStackRequest(String stackName, @Optional("") String credentialName, @Optional("") String region,
            @Optional("") String availabilityZone, @Optional("") String imageCatalog, @Optional("") String imageId) {
        IntegrationTestContext itContext = getItContext();

        credentialName = StringUtils.hasText(credentialName) ? credentialName : itContext.getContextParam(CloudbreakV2Constants.CREDENTIAL_NAME);
        region = StringUtils.hasText(region) ? region : itContext.getContextParam(CloudbreakV2Constants.REGION);
        availabilityZone = StringUtils.hasText(availabilityZone) ? availabilityZone : itContext.getContextParam(CloudbreakV2Constants.AVAILABILTYZONE);
//        imageCatalog = StringUtils.hasText(imageCatalog) ? imageCatalog : itContext.getContextParam(CloudbreakV2Constants.IMAGECATALOG);
//        imageId = StringUtils.hasText(imageId) ? imageId : itContext.getContextParam(CloudbreakV2Constants.IMAGEID);
        Map<String, InstanceGroupV4Request> instanceGroupV2RequestMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);

        Assert.assertTrue(StringUtils.hasText(credentialName), "Credential name is mandatory.");
        Assert.assertTrue(StringUtils.hasText(region), "Region is mandatory.");
        Assert.assertTrue(StringUtils.hasText(availabilityZone), "AvailabilityZone is mandatory.");
        Assert.assertNotNull(instanceGroupV2RequestMap, "InstanceGroup map is mandatory");

        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setName(stackName);
        EnvironmentSettingsV4Request env = new EnvironmentSettingsV4Request();
        env.setCredentialName(credentialName);
        stackRequest.setEnvironment(env);
        PlacementSettingsV4Request ps = new PlacementSettingsV4Request();
        stackRequest.setPlacement(ps);
        ps.setRegion(region);
        ps.setAvailabilityZone(availabilityZone);

//        if (StringUtils.hasText(imageCatalog) || StringUtils.hasText(imageId)) {
//            ImageSettingsV4Request is = new ImageSettingsV4Request();
//            is.setCatalog(imageCatalog);
//            is.setId(imageId);
//        }
        stackRequest.setInstanceGroups(Lists.newArrayList(instanceGroupV2RequestMap.values()));

        itContext.putContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, stackRequest);
    }

    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters("publicKeyId")
    public void authenticationParams(@Optional("") String publicKeyId) {
        IntegrationTestContext itContext = getItContext();
        publicKeyId = StringUtils.hasText(publicKeyId) ? publicKeyId : itContext.getContextParam(CloudbreakV2Constants.SSH_PUBLICKEY_ID);

        Assert.assertNotNull(publicKeyId, "Publickey id is mandatory.");

        StackV4Request stackRequest = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV4Request.class);
        StackAuthenticationV4Request stackAuthenticationRequest = new StackAuthenticationV4Request();
        stackAuthenticationRequest.setPublicKeyId(publicKeyId);
        stackRequest.setAuthentication(stackAuthenticationRequest);
    }

    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters({"clusterDefinitionName", "enableGateway"})
    public void ambariParameters(@Optional("") String clusterDefinitionName, @Optional("true") boolean enableGateway) {
        IntegrationTestContext itContext = getItContext();
        clusterDefinitionName = StringUtils.hasText(clusterDefinitionName)
                ? clusterDefinitionName
                : itContext.getContextParam(CloudbreakV2Constants.CLUSTER_DEFINITION_NAME);

        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password is mandatory.");
        Assert.assertNotNull(clusterDefinitionName, "cluster definition name is mandatory.");

        StackV4Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV4Request.class);
        ClusterV4Request clusterV2Request = new ClusterV4Request();
        clusterV2Request.setName(stackV2Request.getName());
        stackV2Request.setCluster(clusterV2Request);
        AmbariV4Request ambariV2Request = new AmbariV4Request();
        clusterV2Request.setAmbari(ambariV2Request);
        ambariV2Request.setClusterDefinitionName(clusterDefinitionName);
        ambariV2Request.setUserName(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID));
        ambariV2Request.setPassword(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID));
        if (enableGateway) {
            addGatewayRequest(stackV2Request);
        }
    }

    private void addGatewayRequest(StackV4Request stackV2Request) {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        gatewayJson.setPath("gateway-path");
        GatewayTopologyV4Request topology1 = new GatewayTopologyV4Request();
        topology1.setTopologyName("topology1");
        topology1.setExposedServices(Collections.singletonList(ExposedService.AMBARI.getKnoxService()));
        GatewayTopologyV4Request topology2 = new GatewayTopologyV4Request();
        topology2.setTopologyName("topology2");
        topology2.setExposedServices(Collections.singletonList(ExposedService.ALL.getServiceName()));
        gatewayJson.setTopologies(Arrays.asList(topology1, topology2));
        stackV2Request.getCluster().setGateway(gatewayJson);
    }

    @Test
    public void testStackCreation() throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        StackV4Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV4Request.class);
        // WHEN
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String stackId = getCloudbreakClient().stackV4Endpoint().post(workspaceId, stackV2Request).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId, true);
        String stackName = stackV2Request.getName();
        itContext.putContextParam(CloudbreakV2Constants.STACK_NAME, stackName);
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
    }

    protected NetworkV4Request createNetworkRequest(IntegrationTestContext itContext, String subnetCidr) {
        subnetCidr = StringUtils.hasText(subnetCidr) ? subnetCidr : itContext.getContextParam(CloudbreakV2Constants.SUBNET_CIDR);
        Assert.assertNotNull(subnetCidr, "Subnet cidr is mandatory.");
        StackV4Request stackV2Request = itContext.getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV4Request.class);
        NetworkV4Request networkRequest = new NetworkV4Request();
        networkRequest.setSubnetCIDR(subnetCidr);
        stackV2Request.setNetwork(networkRequest);
        return networkRequest;
    }
}
