package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class GroupNetwork extends DynamicModel {
    private final Set<GroupSubnet> subnets;

    private final Set<GroupSubnet> endpointGatewaySubnets;

    private final OutboundInternetTraffic outboundInternetTraffic;

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic, Set<GroupSubnet> subnets, Map<String, Object> parameters) {
        this(outboundInternetTraffic, subnets, new HashSet<>(), parameters);
    }

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic, Set<GroupSubnet> subnets, Set<GroupSubnet> endpointGatewaySubnets,
            Map<String, Object> parameters) {
        super(parameters);
        this.outboundInternetTraffic = outboundInternetTraffic;
        this.subnets = subnets;
        this.endpointGatewaySubnets = endpointGatewaySubnets;
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

    @Override
    public String toString() {
        return "GroupNetwork{" +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                ", subnets=" + subnets +
                ", endpointGatewaySubnets=" + endpointGatewaySubnets +
                '}';
    }
}
