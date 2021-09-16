package com.sequenceiq.cloudbreak.cloud.gcp.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;

public class GcpLoadBalancerMetadataView {

    public static final String LOADBALANCER_NAME = "loadBalancerName";

    public static final String INSTANCE_GROUP_PREFIX = "InstanceGroupName";

    public static final String BACKEND_SERVICE_PREFIX = "BackendServiceName";

    private final CloudLoadBalancerMetadata cloudLoadBalancerMetadata;

    public GcpLoadBalancerMetadataView(CloudLoadBalancerMetadata cloudLoadBalancerMetadata) {
        this.cloudLoadBalancerMetadata = cloudLoadBalancerMetadata;
    }

    public String getLoadbalancerName() {
        return cloudLoadBalancerMetadata.getStringParameter(LOADBALANCER_NAME);
    }

    public String getInstanceGroupByPort(int port) {
        return cloudLoadBalancerMetadata.getStringParameter(getInstanceGroupParam(port));
    }

    public String getBackendServiceByPort(int port) {
        return cloudLoadBalancerMetadata.getStringParameter(getBackendServiceParam(port));
    }

    public static String getInstanceGroupParam(int port) {
        return getInstanceGroupParam(String.valueOf(port));
    }

    public static String getInstanceGroupParam(String port) {
        return INSTANCE_GROUP_PREFIX + port;
    }

    public static String getBackendServiceParam(int port) {
        return getBackendServiceParam(String.valueOf(port));
    }

    public static String getBackendServiceParam(String port) {
        return BACKEND_SERVICE_PREFIX + port;
    }
}
