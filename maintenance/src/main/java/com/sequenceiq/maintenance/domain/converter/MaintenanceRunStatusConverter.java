package com.sequenceiq.maintenance.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;

public class MaintenanceRunStatusConverter extends DefaultEnumConverter<MaintenanceRunStatus> {

    @Override
    public MaintenanceRunStatus getDefault() {
        return MaintenanceRunStatus.SKIPPED;
    }
}
