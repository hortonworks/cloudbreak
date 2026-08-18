package com.sequenceiq.maintenance.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

public class MaintenanceScopeTypeConverter extends DefaultEnumConverter<MaintenanceScopeType> {

    @Override
    public MaintenanceScopeType getDefault() {
        return MaintenanceScopeType.TENANT;
    }
}
