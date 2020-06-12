package com.sequenceiq.cloudbreak.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class OutboundInternetTrafficConverterTest extends DefaultEnumConverterBaseTest<OutboundInternetTraffic> {

    @Override
    public OutboundInternetTraffic getDefaultValue() {
        return OutboundInternetTraffic.ENABLED;
    }

    @Override
    public AttributeConverter<OutboundInternetTraffic, String> getVictim() {
        return new OutboundInternetTrafficConverter();
    }
}