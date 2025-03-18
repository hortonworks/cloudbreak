package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;

public class AwsTargetGroup {

    private static final String TARGET_GROUP_NAME_PREFIX = "TargetGroupPort";

    private final Set<String> instanceIds = new HashSet<>();

    private final int port;

    private ProtocolEnum protocol;

    private final String name;

    private final String healthCheckPort;

    private final String healthCheckPath;

    private final ProtocolEnum healthCheckProtocol;

    private String arn;

    private boolean stickySessionEnabled;

    private final Integer healthCheckIntervalSeconds;

    private final Integer healthCheckThresholdCount;

    public AwsTargetGroup(AwsLoadBalancerScheme scheme, int port, ProtocolEnum protocol, String healthCheckPath, int healthCheckPort,
            ProtocolEnum healthCheckProtocol, boolean stickySessionEnabled, Integer healthCheckIntervalSeconds, Integer healthCheckThresholdCount) {
        this.port = port;
        this.protocol = protocol;
        this.healthCheckPath = healthCheckPath;
        this.healthCheckPort = String.valueOf(healthCheckPort);
        this.healthCheckProtocol = healthCheckProtocol;
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
        this.healthCheckThresholdCount = healthCheckThresholdCount;
        name = getTargetGroupName(port, scheme);
        this.stickySessionEnabled = stickySessionEnabled;
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

    public String getHealthCheckPort() {
        return healthCheckPort;
    }

    public ProtocolEnum getProtocol() {
        return protocol;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public ProtocolEnum getHealthCheckProtocol() {
        return healthCheckProtocol;
    }

    public static String getTargetGroupName(int port, AwsLoadBalancerScheme scheme) {
        return TARGET_GROUP_NAME_PREFIX + port + scheme.resourceName();
    }

    public boolean isStickySessionEnabled() {
        return stickySessionEnabled;
    }

    public void setStickySessionEnabled(boolean stickySessionEnabled) {
        this.stickySessionEnabled = stickySessionEnabled;
    }

    public Integer getHealthCheckIntervalSeconds() {
        return healthCheckIntervalSeconds;
    }

    public Integer getHealthCheckThresholdCount() {
        return healthCheckThresholdCount;
    }
}
