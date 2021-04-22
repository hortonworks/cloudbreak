package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ScalabilityOption;

public class ScalabilityOptionConverter extends DefaultEnumConverter<ScalabilityOption> {

    @Override
    public ScalabilityOption getDefault() {
        return ScalabilityOption.ALLOWED;
    }
}
