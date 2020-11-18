package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AwsTargetGroup {

    private static final String TARGET_GROUP_NAME_PREFIX = "TargetGroupPort";

    private final int port;

    private final String name;

    private final int order;

    private final List<String> instanceIds;

    private String arn;

    public AwsTargetGroup(int port, AwsLoadBalancerScheme scheme, int order, List<String> instanceIds) {
        this.port = port;
        this.order = order;
        this.instanceIds = instanceIds;
        name = getTargetGroupName(port, scheme);
    }

    public int getPort() {
        return port;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    private static String getTargetGroupName(int port, AwsLoadBalancerScheme scheme) {
        return TARGET_GROUP_NAME_PREFIX + port +
            StringUtils.capitalize(scheme.name().toLowerCase());
    }
}
