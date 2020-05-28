package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

public class GatewayTypeConverterTest extends DefaultEnumConverterBaseTest<GatewayType> {

    @Override
    public GatewayType getDefaultValue() {
        return GatewayType.CENTRAL;
    }

    @Override
    public AttributeConverter<GatewayType, String> getVictim() {
        return new GatewayTypeConverter();
    }
}