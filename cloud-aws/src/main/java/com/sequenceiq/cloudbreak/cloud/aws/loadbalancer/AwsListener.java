package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AwsListener {

    private static final String LISTENER_NAME_PREFIX = "ListenerPort";

    private final int port;

    private final List<AwsTargetGroup> targetGroups;

    private final String name;

    public AwsListener(int port, List<AwsTargetGroup> targetGroups, AwsLoadBalancerScheme scheme) {
        this.port = port;
        this.targetGroups = targetGroups;
        this.name = getListenerName(port, scheme);
    }

    public int getPort() {
        return port;
    }

    public List<AwsTargetGroup> getTargetGroups() {
        return targetGroups;
    }

    public boolean areTargetGroupArnsSet() {
        return targetGroups.stream().noneMatch(t -> t.getArn() == null || t.getArn().isEmpty());
    }

    public String getName() {
        return name;
    }

    private static String getListenerName(int port, AwsLoadBalancerScheme scheme) {
        return LISTENER_NAME_PREFIX + port +
            StringUtils.capitalize(scheme.name().toLowerCase());
    }
}
