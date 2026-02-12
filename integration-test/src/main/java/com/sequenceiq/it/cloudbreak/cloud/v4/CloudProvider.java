package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.StackV4ParameterBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.RootVolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.SubnetId;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXRootVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public interface CloudProvider {

    String verticalScaleVolumeType();

    String availabilityZone();

    String region();

    String location();

    String getDefaultInstanceType(Architecture architecture);

    ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog);

    ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings);

    DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings);

    String getLatestPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient);

    String getLatestMarketplacePreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient,
            String runtimeVersion);

    String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient);

    String getLatestBaseImageID(Architecture architecture, TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient);

    String getBaseImageTestCatalogName();

    String getBaseImageTestCatalogUrl();

    InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template);

    DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template, Architecture architecture);

    VolumeV4TestDto attachedVolume(VolumeV4TestDto volume);

    DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume);

    RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume);

    InstanceGroupNetworkV4Request instanceGroupNetworkV4Request(SubnetId subnetId);

    InstanceGroupNetworkV1Request instanceGroupNetworkV1Request(SubnetId subnetId);

    LoggingRequest loggingRequest(TelemetryTestDto dto);

    DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume);

    NetworkV4TestDto network(NetworkV4TestDto network);

    ServiceEndpointCreation serviceEndpoint();

    DistroXNetworkTestDto network(DistroXNetworkTestDto network);

    EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network);

    EnvironmentNetworkTestDto trustSetupNetwork(EnvironmentNetworkTestDto network);

    @Deprecated
    EnvironmentNetworkTestDto newNetwork(EnvironmentNetworkTestDto network);

    TelemetryTestDto telemetry(TelemetryTestDto telemetry);

    EnvironmentTestDto setS3Guard(EnvironmentTestDto environmentTestDto, String tableName);

    EnvironmentTestDto withResourceGroup(EnvironmentTestDto environmentTestDto, String resourceGroupUsage, String resourceGroupName);

    EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto);

    EnvironmentTestDto withDatabaseEncryptionKey(EnvironmentTestDto environmentTestDto);

    EnvironmentTestDto withResourceEncryptionUserManagedIdentity(EnvironmentTestDto environmentTestDto);

    DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase);

    StackTestDtoBase stack(StackTestDtoBase stack);

    ClusterTestDto cluster(ClusterTestDto cluster);

    DistroXTestDtoBase distrox(DistroXTestDtoBase distrox);

    DistroXClusterTestDto cluster(DistroXClusterTestDto cluster);

    SdxTestDto sdx(SdxTestDto sdx);

    SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal);

    SdxRepairTestDto sdxRepair(SdxRepairTestDto sdxRepair);

    default ExternalizedComputeClusterTestDto externalizedComputeCluster(ExternalizedComputeClusterTestDto computeClusterDto) {
        return computeClusterDto;
    }

    SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage);

    FileSystemType getFileSystemType();

    String getBaseLocation();

    String getBaseLocationForPreTermination();

    String getSubnetCIDR();

    String getAccessCIDR();

    Map<String, String> getTags();

    SdxClusterShape getClusterShape();

    SdxClusterShape getInternalClusterShape();

    CloudPlatform getCloudPlatform();

    CredentialTestDto credential(CredentialTestDto credential);

    EnvironmentTestDto environment(EnvironmentTestDto environment);

    PlacementSettingsTestDto placement(PlacementSettingsTestDto placement);

    StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity);

    EnvironmentAuthenticationTestDto environmentAuthentication(EnvironmentAuthenticationTestDto environmentAuthenticationEntity);

    EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccessTestDto);

    Integer gatewayPort(StackTestDtoBase stackEntity);

    Integer gatewayPort(FreeIpaTestDto stackEntity);

    String getDataEngDistroXBlueprintName();

    String getDataMartDistroXBlueprintName();

    String getStreamsHADistroXBlueprintName();

    String getBlueprintCdhVersion();

    StackV4ParameterBase stackParameters();

    CloudFunctionality getCloudFunctionality();

    String getImageCatalogName();

    void setImageCatalogName(String name);

    void setImageCatalogUrl(String url);

    void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request);

    String getFreeIpaImageCatalogUrl();

    String getVariant();

    String getFreeIpaUpgradeImageId();

    String getFreeIpaCentos7UpgradeImageId();

    String getFreeIpaMarketplaceUpgradeImageId();

    String getFreeIpaUpgradeImageCatalog();

    String getFreeIpaMarketplaceUpgradeImageCatalog();

    String getSdxMarketplaceUpgradeImageId();

    String getSdxMarketplaceUpgradeImageCatalog();

    String getStorageOptimizedInstanceType();

    default SdxDatabaseRequest extendDBRequestWithProviderParams(SdxDatabaseRequest sdxDatabaseRequest) {
        return sdxDatabaseRequest;
    }

    VerticalScalingTestDto freeIpaVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto);

    VerticalScalingTestDto distroXVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto);

    VerticalScalingTestDto datalakeVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto);

    void verifyDiskEncryptionKey(DetailedEnvironmentResponse environment, String environmentName);

    void verifyVolumeEncryptionKey(List<String> volumeEncryptionKeyIds, String environmentName);

    boolean getGovCloud();

    boolean isMultiAZ();

    boolean verticalScalingSupported();

    boolean isExternalDatabaseSslEnforcementSupported();

    String getEmbeddedDbUpgradeSourceVersion();

    String getFreeIpaRebuildFullBackup();

    String getFreeIpaRebuildDataBackup();

    String getFreeIpaInstanceType();
}
