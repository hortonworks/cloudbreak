package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String DEFAULT_ACCESS_CIDR = "0.0.0.0/0";

    private static final String CLOUDBREAK_DEFAULT = "cloudbreak-default";

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
        return imageCatalog.withName(CLOUDBREAK_DEFAULT).withUrl(null);
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        imageSettings.withImageCatalog(CLOUDBREAK_DEFAULT);
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
    public String getClusterShape() {
        return commonCloudProperties.getClusterShape();
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return commonCloudProperties.getGatewayPort();
    }

    @Override
    public final ClusterTestDto cluster(ClusterTestDto clusterTestDto) {
        clusterTestDto.withUserName(commonCloudProperties.getAmbari().getDefaultUser())
                .withPassword(commonCloudProperties.getAmbari().getDefaultPassword());
        return withCluster(clusterTestDto);
    }

    @Override
    public final SdxTestDto sdx(SdxTestDto sdx) {
        sdx.withAccessCidr(commonCloudProperties.getAccessCidr()).withTags(commonCloudProperties.getTags());
        return sdx;
    }

    protected abstract ClusterTestDto withCluster(ClusterTestDto cluster);
}
