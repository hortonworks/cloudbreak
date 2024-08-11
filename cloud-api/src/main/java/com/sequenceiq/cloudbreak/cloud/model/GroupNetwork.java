package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class GroupNetwork extends DynamicModel {
    private final Set<GroupSubnet> subnets;

    private final Set<GroupSubnet> endpointGatewaySubnets;

    private final Set<String> availabilityZones;

    private final OutboundInternetTraffic outboundInternetTraffic;

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic, Set<GroupSubnet> subnets, Map<String, Object> parameters) {
        this(outboundInternetTraffic, subnets, new HashSet<>(), new HashSet<>(), parameters);
    }

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic, Set<GroupSubnet> subnets, Map<String, Object> parameters,
            Set<String> availabilityZones) {
        this(outboundInternetTraffic, subnets, new HashSet<>(), availabilityZones, parameters);
    }

    @JsonCreator
    public GroupNetwork(
            @JsonProperty("outboundInternetTraffic") OutboundInternetTraffic outboundInternetTraffic,
            @JsonProperty("subnets") Set<GroupSubnet> subnets,
            @JsonProperty("endpointGatewaySubnets") Set<GroupSubnet> endpointGatewaySubnets,
            @JsonProperty("availabilityZones") Set<String> availabilityZones,
            @JsonProperty("parameters") Map<String, Object> parameters) {

        super(parameters);
        this.outboundInternetTraffic = outboundInternetTraffic;
        this.subnets = subnets;
        this.endpointGatewaySubnets = endpointGatewaySubnets;
        this.availabilityZones = availabilityZones;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public Set<GroupSubnet> getSubnets() {
        return subnets;
    }

    public Set<GroupSubnet> getEndpointGatewaySubnets() {
        return endpointGatewaySubnets;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    @Override
    public String toString() {
        return "GroupNetwork{" +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                ", subnets=" + subnets +
                ", endpointGatewaySubnets=" + endpointGatewaySubnets +
                ", availabilityZones=" + availabilityZones +
                '}';
    }
}
