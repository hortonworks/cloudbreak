package com.sequenceiq.maintenance.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;

public class MaintenanceTaskStatusConverter extends DefaultEnumConverter<MaintenanceTaskStatus> {

    @Override
    public MaintenanceTaskStatus getDefault() {
        return MaintenanceTaskStatus.ACTIVE;
    }
}
