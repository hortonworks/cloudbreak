package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;

public class BlueprintHybridOptionConverter extends DefaultEnumConverter<BlueprintHybridOption> {

    @Override
    public BlueprintHybridOption getDefault() {
        return BlueprintHybridOption.NONE;
    }
}
