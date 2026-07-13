package com.sequenceiq.maintenance.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.maintenance.domain.MaintenanceRecurrenceKind;

public class MaintenanceRecurrenceKindConverter extends DefaultEnumConverter<MaintenanceRecurrenceKind> {

    @Override
    public MaintenanceRecurrenceKind getDefault() {
        return MaintenanceRecurrenceKind.WEEKLY;
    }
}
