package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import java.util.Collections;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

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
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return commonCloudProperties.getGatewayPort();
    }

    @Override
    public final ClusterTestDto cluster(ClusterTestDto clusterTestDto) {
        clusterTestDto.withUserName(commonCloudProperties.getAmbari().getDefaultUser())
                .withPassword(commonCloudProperties.getAmbari().getDefaultPassword());
        return withCluster(clusterTestDto);
    }

    protected abstract ClusterTestDto withCluster(ClusterTestDto cluster);
}
