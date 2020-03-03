package com.sequenceiq.it.cloudbreak.cloud.v4.mock;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.MockStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.MockNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
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
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ImageCatalogEndpoint;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
public class MockCloudProvider extends AbstractCloudProvider {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String EUROPE = "Europe";

    private static final String DEFAULT_BLUEPRINT_CDH_VERSION = "7.0.2";

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private MockProperties mockProperties;

    @Override
    public CredentialTestDto credential(CredentialTestDto credentialEntity) {
        MockParameters credentialParameters = new MockParameters();
        if (credentialEntity.getTestContext() instanceof MockedTestContext) {
            MockedTestContext mockedTestContext = (MockedTestContext) credentialEntity.getTestContext();
            credentialParameters.setMockEndpoint(mockedTestContext.getSparkServer().getEndpoint());
        } else {
            credentialParameters.setMockEndpoint(
                    credentialEntity.getTestContext().get(HttpMock.class).getSparkServer().getEndpoint());
        }
        return credentialEntity.withName(resourcePropertyProvider.getName(getCloudPlatform()))
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
        return distrox.withGatewayPort(getSparkServerPort(distrox.getTestContext()));
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
    public CloudFunctionality getCloudFunctionality() {
        return throwNotImplementedException();
    }

    @Override
    public void setImageId(String id) {
        throwNotImplementedException();
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
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
        if (imageCatalog.getTestContext() instanceof MockedTestContext) {
            MockedTestContext mockedTestContext = (MockedTestContext) imageCatalog.getTestContext();
            imageCatalog.withUrl(mockedTestContext.getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrl());
        } else {
            imageCatalog.withUrl(
                    httpMock -> httpMock.whenRequested(ImageCatalogEndpoint.Base.class).getCatalog().getFullUrl());
        }
        return imageCatalog;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings
                .withImageId("f6e778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        return imageSettings.withImageId("f6e778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(imageSettings.getTestContext().given(ImageSettingsTestDto.class).getName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return throwNotImplementedException();
    }

    private <T> T throwNotImplementedException() {
        throw new NotImplementedException(String.format("Not implemented on %s", getCloudPlatform()));
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return throwNotImplementedException();
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
        return network.withMock(networkParameters());
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network.withMock(distroXNetworkParameters());
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network
                .withSubnetIDs(getSubnetIDs())
                .withMock(getMockNetworkParams());
    }

    private EnvironmentNetworkMockParams getMockNetworkParams() {
        EnvironmentNetworkMockParams environmentNetworkMockParams = new EnvironmentNetworkMockParams();
        environmentNetworkMockParams.setInternetGatewayId(getInternetGatewayId());
        environmentNetworkMockParams.setVpcId(getVpcId());
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
        return getSparkServerPort(stackEntity.getTestContext());
    }

    @Override
    public Integer gatewayPort(FreeIPATestDto stackEntity) {
        return getSparkServerPort(stackEntity.getTestContext());
    }

    @Override
    public String getBlueprintName() {
        return mockProperties.getDefaultBlueprintName();
    }

    @Override
    public String getBlueprintCdhVersion() {
        return DEFAULT_BLUEPRINT_CDH_VERSION;
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

    private Integer getSparkServerPort(TestContext testContext) {
        if (testContext instanceof MockedTestContext) {
            MockedTestContext mockedTestContext = (MockedTestContext) testContext;
            return mockedTestContext.getSparkServer().getPort();
        } else if (testContext.get(HttpMock.class) != null) {
            return testContext.get(HttpMock.class).getSparkServer().getPort();
        } else {
            throw new IllegalArgumentException("There should have HttpMock entity.");
        }
    }
}
