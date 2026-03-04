package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ConfigStalenessState;

public class ConfigStalenessStateConverter extends DefaultEnumConverter<ConfigStalenessState> {
    @Override
    public ConfigStalenessState getDefault() {
        return ConfigStalenessState.UP_TO_DATE;
    }
}
