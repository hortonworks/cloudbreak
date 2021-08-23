package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;

public class AzureLoadBalancerMetadataView {

    public static final String LOADBALANCER_NAME = "loadBalancerName";

    public static final String AVAILABILITY_SET_PREFIX = "availabilitySetName";

    private final CloudLoadBalancerMetadata cloudLoadBalancerMetadata;

    public AzureLoadBalancerMetadataView(CloudLoadBalancerMetadata cloudLoadBalancerMetadata) {
        this.cloudLoadBalancerMetadata = cloudLoadBalancerMetadata;
    }

    public String getLoadbalancerName() {
        return cloudLoadBalancerMetadata.getStringParameter(LOADBALANCER_NAME);
    }

    public String getAvailabilitySetByPort(int port) {
        return cloudLoadBalancerMetadata.getStringParameter(getAvailabilitySetParam(port));
    }

    public static String getAvailabilitySetParam(int port) {
        return AVAILABILITY_SET_PREFIX + port;
    }
}
