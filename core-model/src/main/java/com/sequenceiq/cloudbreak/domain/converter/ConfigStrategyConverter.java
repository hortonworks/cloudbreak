package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ConfigStrategyConverter extends DefaultEnumConverter<ConfigStrategy> {

    @Override
    public ConfigStrategy getDefault() {
        return ConfigStrategy.ALWAYS_APPLY;
    }
}
