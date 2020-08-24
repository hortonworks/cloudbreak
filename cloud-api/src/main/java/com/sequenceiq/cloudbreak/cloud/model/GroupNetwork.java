package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class GroupNetwork extends DynamicModel {

    private final OutboundInternetTraffic outboundInternetTraffic;

    public GroupNetwork() {
        this(OutboundInternetTraffic.ENABLED);
    }

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public GroupNetwork(Map<String, Object> parameters) {
        this(OutboundInternetTraffic.ENABLED, parameters);
    }

    public GroupNetwork(OutboundInternetTraffic outboundInternetTraffic, Map<String, Object> parameters) {
        super(parameters);
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    @Override
    public String toString() {
        return "GroupNetwork{" +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                '}';
    }
}
