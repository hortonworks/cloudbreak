package com.sequenceiq.it.cloudbreak.newway.cloud.v2.provider;

import java.util.Collections;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    @Inject
    private TestParameter testParameter;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        return imageCatalog.withName("cloudbreak-default").withUrl(null);
    }

    @Override
    public EnvironmentEntity environment(EnvironmentEntity environment) {
        return environment.withRegions(Collections.singleton(region()))
                .withLocation(location());
    }

    @Override
    public PlacementSettingsEntity placement(PlacementSettingsEntity placement) {
        return placement.withRegion(region())
                .withAvailabilityZone(availabilityZone());
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = testParameter.get(CommonCloudParameters.SUBNET_CIDR);
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public Integer gatewayPort(StackV4EntityBase stackEntity) {
        String gatewayPort = getTestParameter().getWithDefault(CommonCloudParameters.GATEWAY_PORT, null);
        if (gatewayPort == null) {
            return null;
        }
        return Integer.parseInt(gatewayPort);
    }
}
