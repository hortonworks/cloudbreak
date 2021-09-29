package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.StackV4ParameterBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Component
public class CloudProviderProxy implements CloudProvider {

    private CloudProvider delegate;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private List<CloudProvider> cloudProviders;

    private final Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();

    @PostConstruct
    private void init() {
        cloudProviders.forEach(cloudProvider -> {
            cloudProviderMap.put(cloudProvider.getCloudPlatform(), cloudProvider);
        });
        delegate = cloudProviderMap.get(CloudPlatform.valueOf(commonCloudProperties.getCloudProvider()));
    }

    @Override
    public String availabilityZone() {
        return delegate.availabilityZone();
    }

    @Override
    public String region() {
        return delegate.region();
    }

    @Override
    public String location() {
        return delegate.location();
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        return getDelegate(imageCatalog).imageCatalog(imageCatalog);
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return getDelegate(imageSettings).imageSettings(imageSettings);
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        return getDelegate(imageSettings).imageSettings(imageSettings);
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return getDelegate(imageCatalogTestDto).getPreviousPreWarmedImageID(testContext, imageCatalogTestDto, cloudbreakClient);
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return getDelegate(imageCatalogTestDto).getLatestBaseImageID(testContext, imageCatalogTestDto, cloudbreakClient);
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return getDelegate(template).template(template);
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return getDelegate(template).template(template);
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return getDelegate(stack).stack(stack);
    }

    @Override
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return getDelegate(cluster).cluster(cluster);
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return getDelegate(distrox).distrox(distrox);
    }

    @Override
    public DistroXClusterTestDto cluster(DistroXClusterTestDto cluster) {
        return getDelegate(cluster).cluster(cluster);
    }

    @Override
    public SdxTestDto sdx(SdxTestDto sdx) {
        return getDelegate(sdx).sdx(sdx);
    }

    @Override
    public SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal) {
        return getDelegate(sdxInternal).sdxInternal(sdxInternal);
    }

    @Override
    public SdxCustomTestDto sdxCustom(SdxCustomTestDto sdxCustom) {
        return getDelegate(sdxCustom).sdxCustom(sdxCustom);
    }

    @Override
    public SdxRepairTestDto sdxRepair(SdxRepairTestDto sdxRepair) {
        return getDelegate(sdxRepair).sdxRepair(sdxRepair);
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return getDelegate(cloudStorage).cloudStorage(cloudStorage);
    }

    @Override
    public FileSystemType getFileSystemType() {
        return delegate.getFileSystemType();
    }

    @Override
    public String getBaseLocation() {
        return delegate.getBaseLocation();
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        return getDelegate(volume).attachedVolume(volume);
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        return getDelegate(volume).attachedVolume(volume);
    }

    @Override
    public RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume) {
        return getDelegate(rootVolume).rootVolume(rootVolume);
    }

    @Override
    public DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume) {
        return getDelegate(distroXRootVolume).distroXRootVolume(distroXRootVolume);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return getDelegate(network).network(network);
    }

    @Override
    public ServiceEndpointCreation serviceEndpoint() {
        return delegate.serviceEndpoint();
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return getDelegate(network).network(network);
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return getDelegate(network).network(network);
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    @Override
    public EnvironmentTestDto setS3Guard(EnvironmentTestDto environmentTestDto, String tableName) {
        return delegate.setS3Guard(environmentTestDto, tableName);
    }

    @Override
    public EnvironmentTestDto withResourceGroup(EnvironmentTestDto environmentTestDto, String resourceGroupUsage, String resourceGroupName) {
        return delegate.withResourceGroup(environmentTestDto, resourceGroupUsage, resourceGroupName);
    }

    @Override
    public String getSubnetCIDR() {
        return delegate.getSubnetCIDR();
    }

    @Override
    public String getAccessCIDR() {
        return delegate.getAccessCIDR();
    }

    @Override
    public Map<String, String> getTags() {
        return delegate.getTags();
    }

    @Override
    public SdxClusterShape getClusterShape() {
        return delegate.getClusterShape();
    }

    @Override
    public SdxClusterShape getInternalClusterShape() {
        return delegate.getInternalClusterShape();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return delegate.getCloudPlatform();
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        return getDelegate(credential).credential(credential);
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return getDelegate(environment).environment(environment);
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return getDelegate(placement).placement(placement);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        return getDelegate(stackAuthenticationEntity).stackAuthentication(stackAuthenticationEntity);
    }

    @Override
    public EnvironmentAuthenticationTestDto environmentAuthentication(EnvironmentAuthenticationTestDto environmentAuthenticationEntity) {
        return delegate.environmentAuthentication(environmentAuthenticationEntity);
    }

    @Override
    public EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccessTestDto) {
        return getDelegate(environmentSecurityAccessTestDto).environmentSecurityAccess(environmentSecurityAccessTestDto);
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return getDelegate(stackEntity).gatewayPort(stackEntity);
    }

    @Override
    public Integer gatewayPort(FreeIpaTestDto stackEntity) {
        return getDelegate(stackEntity).gatewayPort(stackEntity);
    }

    @Override
    public String getBlueprintName() {
        return String.format(delegate.getBlueprintName(), commonClusterManagerProperties.getRuntimeVersion());
    }

    @Override
    public String getBlueprintCdhVersion() {
        return delegate.getBlueprintCdhVersion();
    }

    @Override
    public StackV4ParameterBase stackParameters() {
        return delegate.stackParameters();
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return delegate.getCloudFunctionality();
    }

    @Override
    public void setImageCatalogName(String name) {
        delegate.setImageCatalogName(name);
    }

    @Override
    public String getImageCatalogName() {
        return delegate.getImageCatalogName();
    }

    @Override
    public void setImageCatalogUrl(String url) {
        delegate.setImageCatalogUrl(url);
    }

    @Override
    public void setImageId(String id) {
        delegate.setImageId(id);
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
        getDelegate(instanceTemplateV1Request.getCloudPlatform()).setInstanceTemplateV1Parameters(instanceTemplateV1Request);
    }

    @Override
    public String getFreeIpaImageCatalogUrl() {
        return delegate.getFreeIpaImageCatalogUrl();
    }

    @Override
    public InstanceGroupNetworkV4Request instanceGroupNetworkV4Request(SubnetId subnetId) {
        return delegate.instanceGroupNetworkV4Request(subnetId);
    }

    @Override
    public InstanceGroupNetworkV1Request instanceGroupNetworkV1Request(SubnetId subnetId) {
        return delegate.instanceGroupNetworkV1Request(subnetId);
    }

    @Override
    public LoggingRequest loggingRequest(TelemetryTestDto dto) {
        return getDelegate(dto).loggingRequest(dto);
    }

    @Override
    public NetworkRequest networkRequest(FreeIpaTestDto dto) {
        return getDelegate(dto).networkRequest(dto);
    }

    @Override
    public String getVariant() {
        return delegate.getVariant();
    }

    public CloudProvider getDelegate(CloudPlatform cloudPlatform) {
        return cloudProviderMap.getOrDefault(cloudPlatform, delegate);
    }

    private CloudProvider getDelegate(CloudbreakTestDto cloudbreakTestDto) {
        return getDelegate(cloudbreakTestDto.getCloudPlatform());
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        return delegate.getFreeIpaUpgradeImageId();
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        return delegate.getFreeIpaUpgradeImageCatalog();
    }
}
