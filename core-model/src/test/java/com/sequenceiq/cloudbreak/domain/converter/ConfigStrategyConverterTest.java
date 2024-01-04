package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class ConfigStrategyConverterTest extends DefaultEnumConverterBaseTest<ConfigStrategy> {

    @Override
    public ConfigStrategy getDefaultValue() {
        return ConfigStrategy.ALWAYS_APPLY;
    }

    @Override
    public AttributeConverter<ConfigStrategy, String> getVictim() {
        return new ConfigStrategyConverter();
    }
}
