package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;

public class FreeIpaLoadBalancerTypeConverterTest extends DefaultEnumConverterBaseTest<FreeIpaLoadBalancerType> {

    @Override
    public FreeIpaLoadBalancerType getDefaultValue() {
        return FreeIpaLoadBalancerType.getDefault();
    }

    @Override
    public AttributeConverter<FreeIpaLoadBalancerType, String> getVictim() {
        return new FreeIpaLoadBalancerTypeConverter();
    }
}
