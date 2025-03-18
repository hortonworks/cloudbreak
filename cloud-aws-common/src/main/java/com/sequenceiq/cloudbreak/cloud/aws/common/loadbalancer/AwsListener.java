package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import java.util.Set;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;

public class AwsListener {

    public static final String LISTENER_NAME_PREFIX = "ListenerPort";

    private final int port;

    private final ProtocolEnum protocol;

    private final AwsTargetGroup targetGroup;

    private final String name;

    public AwsListener(AwsLoadBalancerScheme scheme, int port, ProtocolEnum protocol, String healthCheckPath, int healthCheckPort,
            ProtocolEnum healthCheckProtocol, boolean stickySessionEnabledForTargetGroup, Integer healthCheckIntervalSeconds,
            Integer healthCheckThresholdCount) {
        this.port = port;
        this.protocol = protocol;
        this.name = getListenerName(port, scheme);
        this.targetGroup = new AwsTargetGroup(scheme, port, protocol, healthCheckPath, healthCheckPort, healthCheckProtocol,
                stickySessionEnabledForTargetGroup, healthCheckIntervalSeconds, healthCheckThresholdCount);
    }

    public int getPort() {
        return port;
    }

    public ProtocolEnum getProtocol() {
        return protocol;
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
