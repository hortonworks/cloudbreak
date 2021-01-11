package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer;

import java.util.HashSet;
import java.util.Set;

public class AwsTargetGroup {

    private static final String TARGET_GROUP_NAME_PREFIX = "TargetGroupPort";

    private final Set<String> instanceIds = new HashSet<>();

    private final int port;

    private final String name;

    private String arn;

    public AwsTargetGroup(AwsLoadBalancerScheme scheme, int port) {
        this.port = port;
        this.name = getTargetGroupName(port, scheme);
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

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public void addInstanceIds(Set<String> newInstanceIds) {
        instanceIds.addAll(newInstanceIds);
    }

    public static String getTargetGroupName(int port, AwsLoadBalancerScheme scheme) {
        return TARGET_GROUP_NAME_PREFIX + port + scheme.resourceName();
    }
}
