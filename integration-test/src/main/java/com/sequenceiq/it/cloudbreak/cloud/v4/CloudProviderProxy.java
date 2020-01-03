package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.StackV4ParameterBase;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Component
public class CloudProviderProxy implements CloudProvider {

    private CloudProvider delegate;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private List<CloudProvider> cloudProviders;

    private final Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();

    @PostConstruct
    private void init() {
        Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();
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
        return delegate.imageCatalog(imageCatalog);
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return delegate.imageSettings(imageSettings);
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        return delegate.imageSettings(imageSettings);
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return delegate.getPreviousPreWarmedImageID(testContext, imageCatalogTestDto, cloudbreakClient);
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return delegate.getLatestBaseImageID(testContext, imageCatalogTestDto, cloudbreakClient);
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return delegate.template(template);
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return delegate.template(template);
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return delegate.stack(stack);
    }

    @Override
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return delegate.cluster(cluster);
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return delegate.distrox(distrox);
    }

    @Override
    public DistroXClusterTestDto cluster(DistroXClusterTestDto cluster) {
        return delegate.cluster(cluster);
    }

    @Override
    public SdxTestDto sdx(SdxTestDto sdx) {
        return delegate.sdx(sdx);
    }

    @Override
    public SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal) {
        return delegate.sdxInternal(sdxInternal);
    }

    @Override
    public SdxRepairTestDto sdxRepair(SdxRepairTestDto sdxRepair) {
        return delegate.sdxRepair(sdxRepair);
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return delegate.cloudStorage(cloudStorage);
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
        return delegate.attachedVolume(volume);
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        return delegate.attachedVolume(volume);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return delegate.network(network);
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return delegate.network(network);
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return delegate.network(network);
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
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
        return delegate.credential(credential);
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return delegate.environment(environment);
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return delegate.placement(placement);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        return delegate.stackAuthentication(stackAuthenticationEntity);
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return delegate.gatewayPort(stackEntity);
    }

    @Override
    public Integer gatewayPort(FreeIPATestDto stackEntity) {
        return delegate.gatewayPort(stackEntity);
    }

    @Override
    public String getBlueprintName() {
        return delegate.getBlueprintName();
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
}
