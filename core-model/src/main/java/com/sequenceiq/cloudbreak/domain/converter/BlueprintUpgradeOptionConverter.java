package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

public class BlueprintUpgradeOptionConverter extends DefaultEnumConverter<BlueprintUpgradeOption> {

    @Override
    public BlueprintUpgradeOption getDefault() {
        return BlueprintUpgradeOption.ENABLED;
    }
}
