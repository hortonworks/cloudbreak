package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class Network extends DynamicModel {
    private final Subnet subnet;

    private final List<String> networkCidrs;

    private final OutboundInternetTraffic outboundInternetTraffic;

    public Network(Subnet subnet) {
        this(subnet, List.of(), OutboundInternetTraffic.ENABLED);
    }

    public Network(Subnet subnet, List<String> networkCidrs, OutboundInternetTraffic outboundInternetTraffic) {
        this.subnet = subnet;
        this.networkCidrs = networkCidrs;
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public Network(Subnet subnet, Map<String, Object> parameters) {
        this(subnet, List.of(), OutboundInternetTraffic.ENABLED, parameters);
    }

    public Network(Subnet subnet, List<String> networkCidrs, OutboundInternetTraffic outboundInternetTraffic, Map<String, Object> parameters) {
        super(parameters);
        this.subnet = subnet;
        this.networkCidrs = networkCidrs;
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public List<String> getNetworkCidrs() {
        return networkCidrs;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    @Override
    public String toString() {
        return "Network{" +
                "subnet=" + subnet +
                ", networkCidrs=" + networkCidrs +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                '}';
    }
}
