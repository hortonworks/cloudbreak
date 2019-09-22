package com.sequenceiq.it.cloudbreak.cloud.v4.mock;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.MockStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.network.MockNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
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
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;

@Component
public class MockCloudProvider extends AbstractCloudProvider {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String EUROPE = "Europe";

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private MockProperties mockProperties;

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
        return mockProperties.getAvailabilityZone();
    }

    @Override
    public String region() {
        return mockProperties.getRegion();
    }

    public String location() {
        return mockProperties.getLocation();
    }

    public String getVpcId() {
        return mockProperties.getVpcId();
    }

    public Set<String> getSubnetIDs() {
        return mockProperties.getSubnetIds();
    }

    public String getSubnetId() {
        Set<String> subnetIDs = mockProperties.getSubnetIds();
        return subnetIDs.iterator().next();
    }

    public String getInternetGatewayId() {
        return mockProperties.getInternetGateway();
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
        return network
                .withSubnetCIDR(getSubnetCIDR())
                .withMock(networkParameters());
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network.withMock(distroXNetworkParameters());
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withNetworkCIDR(getSubnetCIDR())
                .withSubnetIDs(getSubnetIDs())
                .withMock(getMockNetworkParams());
    }

    private EnvironmentNetworkMockParams getMockNetworkParams() {
        EnvironmentNetworkMockParams environmentNetworkMockParams = new EnvironmentNetworkMockParams();
        environmentNetworkMockParams.setVpcId(getVpcId());
        environmentNetworkMockParams.setInternetGatewayId(getInternetGatewayId());
        return environmentNetworkMockParams;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
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
        stackAuthenticationEntity.withPublicKeyId(mockProperties.getPublicKeyId());
        return stackAuthenticationEntity;
    }

    public EnvironmentNetworkTestDto environmentNetwork(EnvironmentNetworkTestDto environmentNetwork) {
        return environmentNetwork
                .withNetworkCIDR(getSubnetCIDR())
                .withSubnetIDs(getSubnetIDs())
                .withMock(environmentNetworkParameters());
    }

    private EnvironmentNetworkMockParams environmentNetworkParameters() {
        EnvironmentNetworkMockParams params = new EnvironmentNetworkMockParams();
        params.setVpcId(getVpcId());
        params.setInternetGatewayId(getInternetGatewayId());
        return params;
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
        return mockProperties.getDefaultBlueprintName();
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

    private MockNetworkV1Parameters distroXNetworkParameters() {
        MockNetworkV1Parameters params = new MockNetworkV1Parameters();
        params.setSubnetId(getSubnetId());
        params.setInternetGatewayId(getInternetGatewayId());
        params.setVpcId(getVpcId());
        return params;
    }
}
