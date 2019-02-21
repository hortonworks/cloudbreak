package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

@Component
public class MockCloudProvider extends AbstractCloudProvider {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String MOCK_CLUSTER_DEFAULT_NAME = "autotesting-mock-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    public static final String CREDENTIAL_DEFAULT_NAME = "autotesting-mock-cred";

    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "autotesting mock credential";

    public static final String NETWORK_DEFAULT_NAME = "autotesting-aws-net";

    public static final String VPC_DEFAULT_ID = "vpc-e623b28d";

    public static final String INTERNET_GATEWAY_ID = "igw-b55b26dd";

    public static final String SUBNET_DEFAULT_ID = "subnet-83901cfe";

    public static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    public static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting mock network";

    public static final String EUROPE = "Europe";

    public static final Set<String> VALID_REGION = Collections.singleton(EUROPE);

    public static final String AVAILABILITY_ZONE = "London";

    public static final String LONDON = "London";

    @Inject
    private RandomNameCreator randomNameCreator;

    @Override
    public CredentialTestDto credential(CredentialTestDto credentialEntity) {
        MockCredentialV4Parameters credentialParameters = new MockCredentialV4Parameters();
        MockedTestContext mockedTestContext = (MockedTestContext) credentialEntity.getTestContext();
        credentialParameters.setMockEndpoint(mockedTestContext.getSparkServer().getEndpoint());
        return credentialEntity.withName(randomNameCreator.getRandomNameForResource())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withMockParameters(credentialParameters)
                .withCloudPlatform(MOCK_CAPITAL);
    }

    //    @Override
    public String availabilityZone() {
        String availabilityZone = "eu-west-1a";
        String availabilityZoneParam = getTestParameter().get("mockAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    //    @Override
    public String region() {
        String regionParam = getTestParameter().get("mockRegion");
        return regionParam == null ? EUROPE : regionParam;
    }

    public String getVpcId() {
        String vpcId = getTestParameter().get("mockVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    public String getSubnetId() {
        String subnetId = getTestParameter().get("mockSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public String getInternetGatewayId() {
        String gatewayId = getTestParameter().get("mockInternetGatewayId");
        return gatewayId == null ? INTERNET_GATEWAY_ID : gatewayId;
    }

    public MockNetworkV4Parameters networkParameters() {
        var parameters = new MockNetworkV4Parameters();
        parameters.setInternetGatewayId(getInternetGatewayId());
        parameters.setVpcId(getVpcId());
        parameters.setSubnetId(getSubnetId());
        return parameters;
    }

    public Object subnetProperties() {
        var parameters = new MockNetworkV4Parameters();
        parameters.setSubnetId(getSubnetId());
        parameters.setVpcId(getVpcId());
        return parameters;
    }

    public NetworkV2Entity existingSubnet(TestContext testContext) {
        var network = testContext.given(NetworkV2Entity.class);
        network.getRequest().setMock((MockNetworkV4Parameters) subnetProperties());
        return network;
    }

    @Override
    public ImageCatalogEntity imageCatalog(ImageCatalogEntity imageCatalog) {
        return imageCatalog;
    }

    @Override
    public InstanceTemplateV4Entity template(InstanceTemplateV4Entity template) {
        return template.withInstanceType("large");
    }

    @Override
    public VolumeV4Entity attachedVolume(VolumeV4Entity volume) {
        return volume
                .withCount(1)
                .withSize(100)
                .withType("magnetic");
    }

    @Override
    public NetworkV2Entity network(NetworkV2Entity network) {
        return network.withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

    @Override
    public EnvironmentEntity environment(EnvironmentEntity environment) {
        return environment
                .withRegions(VALID_REGION)
                .withLocation(LONDON);
    }

    @Override
    public PlacementSettingsEntity placement(PlacementSettingsEntity placement) {
        return placement.withRegion(region())
                .withAvailabilityZone(availabilityZone());
    }

    @Override
    public StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity) {
        return stackAuthenticationEntity;
    }

    @Override
    public Integer gatewayPort(StackV4EntityBase stackEntity) {
        MockedTestContext mockedTestContext = (MockedTestContext) stackEntity.getTestContext();
        return mockedTestContext.getSparkServer().getPort();
    }
}