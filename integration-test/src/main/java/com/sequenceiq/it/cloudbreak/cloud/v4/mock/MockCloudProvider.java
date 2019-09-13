package com.sequenceiq.it.cloudbreak.cloud.v4.mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.MockStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;

@Component
public class MockCloudProvider extends AbstractCloudProvider {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String MOCK_CLUSTER_DEFAULT_NAME = "autotesting-mock-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    public static final String CREDENTIAL_DEFAULT_NAME = "autotesting-mock-cred";

    public static final String NETWORK_DEFAULT_NAME = "autotesting-aws-net";

    public static final String VPC_DEFAULT_ID = "vpc-e623b28d";

    public static final String INTERNET_GATEWAY_ID = "igw-b55b26dd";

    public static final String SUBNET_DEFAULT_ID = "subnet-83901cfe";

    public static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting mock network";

    public static final String EUROPE = "Europe";

    public static final Set<String> VALID_REGION = Collections.singleton(EUROPE);

    public static final String AVAILABILITY_ZONE = "London";

    public static final String LONDON = "London";

    public static final String DEFAULT_CLUSTER_DEFINTION_NAME = "CDP 1.1 - Data Engineering: Apache Spark, Apache Hive, Apache Oozie";

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Override
    public CredentialTestDto credential(CredentialTestDto credentialEntity) {
        MockParameters credentialParameters = new MockParameters();
        MockedTestContext mockedTestContext = (MockedTestContext) credentialEntity.getTestContext();
        credentialParameters.setMockEndpoint(mockedTestContext.getSparkServer().getEndpoint());
        return credentialEntity.withName(resourcePropertyProvider.getName())
                .withDescription(commonCloudProperties().getDefaultCredentialDescription())
                .withMockParameters(credentialParameters)
                .withCloudPlatform(MOCK_CAPITAL);
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withMock(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return distrox;
    }

    @Override
    public DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getBlueprintName());
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.TRUE)
                .withBlueprintName(getBlueprintName());
    }

    @Override
    public MockStackV4Parameters stackParameters() {
        return new MockStackV4Parameters();
    }

    @Override
    public String availabilityZone() {
        String availabilityZone = "eu-west-1a";
        String availabilityZoneParam = getTestParameter().get("mockAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public String region() {
        String regionParam = getTestParameter().get("mockRegion");
        return regionParam == null ? EUROPE : regionParam;
    }

    public String location() {
        String locationParam = getTestParameter().get("mockLocation");
        return locationParam == null ? LONDON : locationParam;
    }

    public String getVpcId() {
        String vpcId = getTestParameter().get("mockVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    public String getSubnetId() {
        String subnetId = getTestParameter().get("mockSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public Set<String> getSubnetIDs() {
        Set<String> subnetIDAsSet = new HashSet<>();
        subnetIDAsSet.add(getSubnetId());
        return subnetIDAsSet;
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

    public NetworkV4TestDto existingSubnet(TestContext testContext) {
        var network = testContext.given(NetworkV4TestDto.class);
        network.getRequest().setMock((MockNetworkV4Parameters) subnetProperties());
        return network;
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        MockedTestContext mockedTestContext = (MockedTestContext) imageCatalog.getTestContext();
        imageCatalog.withUrl(mockedTestContext.getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrl());
        return imageCatalog;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings.withImageId("f6e778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(imageSettings.getTestContext().given(ImageSettingsTestDto.class).getName());
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType("large");
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return template.withInstanceType("large");
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        return volume
                .withCount(1)
                .withSize(100)
                .withType("magnetic");
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        return volume
                .withCount(1)
                .withSize(100)
                .withType("magnetic");
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return network.withSubnetCIDR(getSubnetCIDR())
                .withMock(networkParameters());
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withNetworkCIDR(getSubnetCIDR())
                .withSubnetIDs(getSubnetIDs());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return environment
                .withRegions(VALID_REGION)
                .withLocation(LONDON);
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return placement.withRegion(region())
                .withAvailabilityZone(availabilityZone());
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        stackAuthenticationEntity.withPublicKeyId("publicKeyId");
        return stackAuthenticationEntity;
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        MockedTestContext mockedTestContext = (MockedTestContext) stackEntity.getTestContext();
        return mockedTestContext.getSparkServer().getPort();
    }

    @Override
    public Integer gatewayPort(FreeIPATestDto stackEntity) {
        MockedTestContext mockedTestContext = (MockedTestContext) stackEntity.getTestContext();
        return mockedTestContext.getSparkServer().getPort();
    }

    @Override
    public String getBlueprintName() {
        return DEFAULT_CLUSTER_DEFINTION_NAME;
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage;
    }

    @Override
    public FileSystemType getFileSystemType() {
        return null;
    }

    @Override
    public String getBaseLocation() {
        return null;
    }

    @Override
    public String getInstanceProfile() {
        return null;
    }
}
