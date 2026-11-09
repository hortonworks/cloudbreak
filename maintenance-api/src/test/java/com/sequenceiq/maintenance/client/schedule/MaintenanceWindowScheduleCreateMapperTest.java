package com.sequenceiq.maintenance.client.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleOnCreateRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;

class MaintenanceWindowScheduleCreateMapperTest {

    @Test
    void mapsOnCreateFieldsToScopedCreateRequest() {
        MaintenanceWindowScheduleOnCreateRequest source = weeklySchedule();
        MaintenanceWindowScheduleRequest target = MaintenanceWindowScheduleCreateMapper.toCreateRequest(
                source, MaintenanceScopeType.DATAHUB, "crn:cdp:datahub:us-west-1:acc:cluster:dh1");

        assertThat(target.getScopeType()).isEqualTo(MaintenanceScopeType.DATAHUB.name());
        assertThat(target.getScopeId()).isEqualTo("crn:cdp:datahub:us-west-1:acc:cluster:dh1");
        assertThat(target.getRecurrenceKind()).isEqualTo("WEEKLY");
        assertThat(target.getDurationMinutes()).isEqualTo(60);
        assertThat(target.getStartLocalTime()).isEqualTo("09:00");
        assertThat(target.getDayOfWeek()).isEqualTo("MONDAY");
    }

    private static MaintenanceWindowScheduleOnCreateRequest weeklySchedule() {
        MaintenanceWindowScheduleOnCreateRequest request = new MaintenanceWindowScheduleOnCreateRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setDurationMinutes(60);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        return request;
    }
}
