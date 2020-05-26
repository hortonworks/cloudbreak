package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class OutboundInternetTrafficConverter extends DefaultEnumConverter<OutboundInternetTraffic> {
    @Override
    public OutboundInternetTraffic getDefault() {
        return OutboundInternetTraffic.ENABLED;
    }
}
