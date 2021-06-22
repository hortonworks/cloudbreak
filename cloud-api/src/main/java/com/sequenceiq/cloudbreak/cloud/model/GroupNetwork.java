package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class GroupNetwork extends DynamicModel {
    private final Set<GroupSubnet> subnets;

    private final OutboundInternetTraffic outboundInternetTraffic;

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic, Set<GroupSubnet> subnets, Map<String, Object> parameters) {
        super(parameters);
        this.outboundInternetTraffic = outboundInternetTraffic;
        this.subnets = subnets;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public Set<GroupSubnet> getSubnets() {
        return subnets;
    }

    @Override
    public String toString() {
        return "GroupNetwork{" +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                ", subnets=" + subnets +
                '}';
    }
}
