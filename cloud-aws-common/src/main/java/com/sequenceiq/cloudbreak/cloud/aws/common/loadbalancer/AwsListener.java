package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import java.util.Set;

public class AwsListener {

    private static final String LISTENER_NAME_PREFIX = "ListenerPort";

    private final int port;

    private final AwsTargetGroup targetGroup;

    private final String name;

    public AwsListener(AwsLoadBalancerScheme scheme, int port, int healthCheckPort) {
        this.port = port;
        this.name = getListenerName(port, scheme);
        this.targetGroup = new AwsTargetGroup(scheme, port, healthCheckPort);
    }

    public int getPort() {
        return port;
    }

    public AwsTargetGroup getTargetGroup() {
        return targetGroup;
    }

    public void addInstancesToTargetGroup(Set<String> instanceIds) {
        targetGroup.addInstanceIds(instanceIds);
    }

    public boolean areTargetGroupArnsSet() {
        return targetGroup.getArn() != null && !targetGroup.getArn().isEmpty();
    }

    public String getName() {
        return name;
    }

    public static String getListenerName(int port, AwsLoadBalancerScheme scheme) {
        return LISTENER_NAME_PREFIX + port + scheme.resourceName();
    }
}
