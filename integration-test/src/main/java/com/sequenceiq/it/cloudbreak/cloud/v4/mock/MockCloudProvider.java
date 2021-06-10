package com.sequenceiq.it.cloudbreak.cloud.v4.mock;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.MockStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.MockNetworkV1Parameters;
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
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
public class MockCloudProvider extends AbstractCloudProvider {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String LONDON = "London";

    private static final String DEFAULT_BLUEPRINT_CDH_VERSION = "7.0.2";

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCloudProvider.class);

    private static final String DEFAULT_STORAGE_NAME = "testsdx" + UUID.randomUUID().toString().replaceAll("-", "");

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private MockProperties mockProperties;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Override
    public CredentialTestDto credential(CredentialTestDto credentialEntity) {
        MockParameters credentialParameters = new MockParameters();
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
    public CloudFunctionality getCloudFunctionality() {
        return throwNotImplementedException();
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
            imageCatalog.withUrl(imageCatalogMockServerSetup.getPreWarmedImageCatalogUrl());
        } else {
            imageCatalog.withUrl(String.format("https://%s:%d", mockInfrastructureHost, 10090));
        }
        return imageCatalog;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        LOGGER.info("Image Catalog Name: {} || Base image UUID: {}", commonCloudProperties().getImageCatalogName(),
                mockProperties.getBaseimage().getRedhat7().getImageId());
        return imageSettings
                .withImageId(mockProperties.getBaseimage().getRedhat7().getImageId())
                .withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        LOGGER.info("Image Catalog Name: {} || Base image UUID: {}", imageSettings.getTestContext().given(ImageSettingsTestDto.class).getName(),
                mockProperties.getBaseimage().getRedhat7().getImageId());
        return imageSettings
                .withImageId(mockProperties.getBaseimage().getRedhat7().getImageId())
                .withImageCatalog(imageSettings.getTestContext().given(ImageSettingsTestDto.class).getName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (mockProperties.getBaseimage().getRedhat7().getImageId() == null || mockProperties.getBaseimage().getRedhat7().getImageId().isEmpty()) {
            try {
                List<ImageV4Response> images = cloudbreakClient
                        .getDefaultClient()
                        .imageCatalogV4Endpoint()
                        .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                                CloudPlatform.MOCK.name(), null, null).getCdhImages();

                ImageV4Response olderImage = images.get(images.size() - 2);
                Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
                Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
                Log.log(LOGGER, format(" Selected Pre-warmed Image Date: %s | ID: %s | Description: %s | Stack Version: %s ", olderImage.getDate(),
                        olderImage.getUuid(), olderImage.getStackDetails().getVersion(), olderImage.getDescription()));
                mockProperties.getBaseimage().getRedhat7().setImageId(olderImage.getUuid());

                return olderImage.getUuid();
            } catch (Exception e) {
                LOGGER.error("Cannot fetch pre-warmed images of {} image catalog!", imageCatalogTestDto.getRequest().getName());
                throw new TestFailException(" Cannot fetch pre-warmed images of " + imageCatalogTestDto.getRequest().getName() + " image catalog!", e);
            }
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", mockProperties.getBaseimage().getRedhat7().getImageId()));
            return mockProperties.getBaseimage().getRedhat7().getImageId();
        }
    }

    private <T> T throwNotImplementedException() {
        throw new NotImplementedException(String.format("Not implemented on %s. Do you want to use against a real provider? You should set the " +
                "`integrationtest.cloudProvider` property, Values: AZURE, AWS", getCloudPlatform()));
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (mockProperties.getBaseimage().getRedhat7().getImageId() == null || mockProperties.getBaseimage().getRedhat7().getImageId().isEmpty()) {
            String imageId = getLatestBaseImage(imageCatalogTestDto, cloudbreakClient, CloudPlatform.MOCK.name());
            mockProperties.getBaseimage().getRedhat7().setImageId(imageId);
            return imageId;
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", mockProperties.getBaseimage().getRedhat7().getImageId()));
            return mockProperties.getBaseimage().getRedhat7().getImageId();
        }
    }

    public String getImageCatalogUrl() {
        return commonCloudProperties().getImageCatalogUrl();
    }

    @Override
    public String getFreeIpaImageCatalogUrl() {
        return imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl();
    }

    @Override
    public void setImageId(String id) {
        LOGGER.info("New base image UUID: {}", id);
        mockProperties.getBaseimage().getRedhat7().setImageId(id);
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
    public String getBlueprintName() {
        return commonClusterManagerProperties().getInternalDistroXBlueprintName();
    }

    @Override
    public String getBlueprintCdhVersion() {
        return DEFAULT_BLUEPRINT_CDH_VERSION;
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage
                .withFileSystemType(getFileSystemType())
                .withBaseLocation(getBaseLocation())
                .withS3(s3CloudStorageParameters());
    }

    public S3CloudStorageV1Parameters s3CloudStorageParameters() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile(mockProperties.getCloudStorage().getS3().getInstanceProfile());
        return s3CloudStorageV1Parameters;
    }

    @Override
    public FileSystemType getFileSystemType() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        return s3CloudStorageV1Parameters.getType();
    }

    @Override
    public String getBaseLocation() {
        return String.join("/", mockProperties.getCloudStorage().getBaseLocation(), DEFAULT_STORAGE_NAME);
    }

    public String getInstanceProfile() {
        return mockProperties.getCloudStorage().getS3().getInstanceProfile();
    }

    private MockNetworkV1Parameters distroXNetworkParameters() {
        MockNetworkV1Parameters params = new MockNetworkV1Parameters();
        params.setSubnetId(getSubnetId());
        params.setInternetGatewayId(getInternetGatewayId());
        params.setVpcId(getVpcId());
        return params;
    }
}
