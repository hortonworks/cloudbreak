package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;

public class AwsLoadBalancerMetadataView {

    public static final String LOADBALANCER_ARN = "loadBalancerArn";

    public static final String LISTENER_ARN_PREFIX = "listenerArn";

    public static final String TARGET_GROUP_ARN_PREFIX = "targetGroupArn";

    private final CloudLoadBalancerMetadata cloudLoadBalancerMetadata;

    public AwsLoadBalancerMetadataView(CloudLoadBalancerMetadata cloudLoadBalancerMetadata) {
        this.cloudLoadBalancerMetadata = cloudLoadBalancerMetadata;
    }

    public String getLoadbalancerArn() {
        return cloudLoadBalancerMetadata.getStringParameter(LOADBALANCER_ARN);
    }

    public String getListenerArnByPort(int port) {
        return cloudLoadBalancerMetadata.getStringParameter(getListenerParam(port));
    }

    public String getTargetGroupArnByPort(int port) {
        return cloudLoadBalancerMetadata.getStringParameter(getTargetGroupParam(port));
    }

    public static String getListenerParam(int port) {
        return LISTENER_ARN_PREFIX + port;
    }

    public static String getTargetGroupParam(int port) {
        return TARGET_GROUP_ARN_PREFIX + port;
    }
}
