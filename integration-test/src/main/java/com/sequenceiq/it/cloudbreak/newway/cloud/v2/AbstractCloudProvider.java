package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import java.util.Collections;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String CLOUDBREAK_DEFAULT = "cloudbreak-default";

    @Inject
    private TestParameter testParameter;

    protected TestParameter getTestParameter() {
        return testParameter;
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
        String subnetCIDR = testParameter.get(CommonCloudParameters.SUBNET_CIDR);
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        String gatewayPort = getTestParameter().getWithDefault(CommonCloudParameters.GATEWAY_PORT, null);
        if (gatewayPort == null) {
            return null;
        }
        return Integer.parseInt(gatewayPort);
    }
}
