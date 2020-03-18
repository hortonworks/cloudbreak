package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String DEFAULT_ACCESS_CIDR = "0.0.0.0/0";

    @Inject
    private TestParameter testParameter;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    protected CommonCloudProperties commonCloudProperties() {
        return commonCloudProperties;
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        return imageCatalog.withName(commonCloudProperties.getImageCatalogName());
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        imageSettings.withImageCatalog(commonCloudProperties.getImageCatalogName());
        return imageSettings;
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        imageSettings.withImageCatalog(commonCloudProperties.getImageCatalogName());
        return imageSettings;
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return environment.withRegions(Collections.singleton(region()))
                .withLocation(location());
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return placement.withRegion(region())
                .withAvailabilityZone(availabilityZone());
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = commonCloudProperties.getSubnetCidr();
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getAccessCIDR() {
        String accessCIDR = commonCloudProperties.getAccessCidr();
        return accessCIDR == null ? DEFAULT_ACCESS_CIDR : accessCIDR;
    }

    @Override
    public Map<String, String> getTags() {
        return commonCloudProperties.getTags();
    }

    @Override
    public SdxClusterShape getClusterShape() {
        return commonCloudProperties.getClusterShape();
    }

    @Override
    public SdxClusterShape getInternalClusterShape() {
        return commonCloudProperties.getInternalClusterShape();
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return commonCloudProperties.getGatewayPort();
    }

    @Override
    public Integer gatewayPort(FreeIPATestDto stackEntity) {
        return commonCloudProperties.getGatewayPort();
    }

    @Override
    public void setImageCatalogName(String name) {
        commonCloudProperties().setImageCatalogName(name);
    }

    @Override
    public void setImageCatalogUrl(String url) {
        commonCloudProperties().setImageCatalogUrl(url);
    }

    @Override
    public String getImageCatalogName() {
        return commonCloudProperties().getImageCatalogName();
    }

    @Override
    public final ClusterTestDto cluster(ClusterTestDto clusterTestDto) {
        clusterTestDto.withUserName(commonCloudProperties.getAmbari().getDefaultUser())
                .withPassword(commonCloudProperties.getAmbari().getDefaultPassword());
        return withCluster(clusterTestDto);
    }

    @Override
    public final DistroXClusterTestDto cluster(DistroXClusterTestDto clusterTestDto) {
        clusterTestDto.withUserName(commonCloudProperties.getAmbari().getDefaultUser())
                .withPassword(commonCloudProperties.getAmbari().getDefaultPassword());
        return withCluster(clusterTestDto);
    }

    @Override
    public final SdxTestDto sdx(SdxTestDto sdx) {
        sdx.withTags(commonCloudProperties.getTags());
        return sdx;
    }

    @Override
    public final SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal) {
        sdxInternal.withDefaultSDXSettings();
        return sdxInternal;
    }

    @Override
    public final SdxRepairTestDto sdxRepair(SdxRepairTestDto sdxRepair) {
        sdxRepair.withHostGroupNames(List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()));
        return sdxRepair;
    }

    protected abstract ClusterTestDto withCluster(ClusterTestDto cluster);

    protected abstract DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster);
}
