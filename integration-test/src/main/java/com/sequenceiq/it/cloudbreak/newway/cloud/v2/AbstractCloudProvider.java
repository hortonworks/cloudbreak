package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.CommonCloudParameters;

public abstract class AbstractCloudProvider implements CloudProvider {

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    @Inject
    private TestParameter testParameter;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    public String getSubnetCIDR() {
        String subnetCIDR = testParameter.get(CommonCloudParameters.SUBNET_CIDR);
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }
}
