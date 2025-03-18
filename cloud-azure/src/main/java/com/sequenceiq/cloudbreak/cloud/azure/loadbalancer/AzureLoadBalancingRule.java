package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;

public final class AzureLoadBalancingRule {
    private final String name;

    private final int backendPort;

    private final int frontendPort;

    private final String protocol;

    private final AzureLoadBalancerProbe probe;

    private final String groupName;

    public AzureLoadBalancingRule(TargetGroupPortPair portPair, Group group) {
        this(portPair, portPair.getTrafficProtocol(), group);
    }

    public AzureLoadBalancingRule(TargetGroupPortPair portPair, NetworkProtocol trafficProtocol, Group group) {
        this.backendPort = portPair.getTrafficPort();
        this.frontendPort = portPair.getTrafficPort();

        this.protocol = Optional.ofNullable(trafficProtocol).map(NetworkProtocol::name).orElse("");
        this.name = defaultNameFromPort(portPair.getTrafficPort(), protocol);
        String healthCheckProtocol = Optional.ofNullable(portPair)
                .map(TargetGroupPortPair::getHealthProbeParameters)
                .map(HealthProbeParameters::getProtocol).map(NetworkProtocol::name).orElse("");
        HealthProbeParameters healthProbeParameters = portPair.getHealthProbeParameters();
        this.probe = new AzureLoadBalancerProbe(
                portPair.getHealthCheckPort(),
                null,
                healthProbeParameters.getPath(),
                healthCheckProtocol,
                healthProbeParameters.getInterval(),
                healthProbeParameters.getProbeDownThreshold());
        this.groupName = checkNotNull(group, "Group must be provided.").getName();
    }

    private String defaultNameFromPort(int port, String trafficProtocol) {

        return "port-" + port + trafficProtocol + "-rule";
    }

    public String getName() {
        return name;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public int getFrontendPort() {
        return frontendPort;
    }

    public AzureLoadBalancerProbe getProbe() {
        return probe;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getProtocol() {
        return protocol;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AzureLoadBalancingRule {\n");
        sb.append("    name: ").append(name).append("\n");
        sb.append("    backendPort: ").append(backendPort).append("\n");
        sb.append("    frontendPort: ").append(frontendPort).append("\n");
        sb.append("    probe: ").append(toIndentedString(probe)).append("\n");
        sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
        sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
