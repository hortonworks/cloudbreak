package com.sequenceiq.maintenance.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;

public class MaintenanceTaskKindConverter extends DefaultEnumConverter<MaintenanceTaskKind> {

    @Override
    public MaintenanceTaskKind getDefault() {
        return MaintenanceTaskKind.EVERY_WINDOW;
    }
}
