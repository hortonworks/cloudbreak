package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class SSOTypeConverterTest extends DefaultEnumConverterBaseTest<SSOType> {

    @Override
    public SSOType getDefaultValue() {
        return SSOType.NONE;
    }

    @Override
    public AttributeConverter<SSOType, String> getVictim() {
        return new SSOTypeConverter();
    }
}