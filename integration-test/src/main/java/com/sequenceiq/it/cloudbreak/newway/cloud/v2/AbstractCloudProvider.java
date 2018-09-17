package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    @Inject
    private TestParameter testParameter;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = testParameter.get("mockSubnetCIDR");
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }
}
