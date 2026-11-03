package com.sequenceiq.environment.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentMaintenanceWindowScheduleEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRestoreRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

class EnvironmentMaintenanceWindowScheduleMapperTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env-1";

    @Test
    void toCreateRequestSetsEnvironmentScopeAndCopiesFields() {
        EnvironmentMaintenanceWindowScheduleRequest source = new EnvironmentMaintenanceWindowScheduleRequest();
        source.setName("nightly");
        source.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        source.setDurationMinutes(90);
        source.setStartLocalTime("01:30");
        source.setDayOfWeek("SATURDAY");

        MaintenanceWindowScheduleRequest result = EnvironmentMaintenanceWindowScheduleMapper.toCreateRequest(source, ENV_CRN);

        assertThat(result.getScopeType()).isEqualTo(MaintenanceScopeType.ENVIRONMENT.name());
        assertThat(result.getScopeId()).isEqualTo(ENV_CRN);
        assertThat(result.getName()).isEqualTo("nightly");
        assertThat(result.getDurationMinutes()).isEqualTo(90);
        assertThat(result.getStartLocalTime()).isEqualTo("01:30");
        assertThat(result.getDayOfWeek()).isEqualTo("SATURDAY");
    }

    @Test
    void toCreateRequestFromEditRequestSetsEnvironmentScopeAndCopiesFields() {
        EnvironmentMaintenanceWindowScheduleEditRequest source = new EnvironmentMaintenanceWindowScheduleEditRequest();
        source.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        source.setDurationMinutes(90);

        MaintenanceWindowScheduleRequest result = EnvironmentMaintenanceWindowScheduleMapper.toCreateRequest(source, ENV_CRN);

        assertThat(result.getScopeType()).isEqualTo(MaintenanceScopeType.ENVIRONMENT.name());
        assertThat(result.getScopeId()).isEqualTo(ENV_CRN);
        assertThat(result.getDurationMinutes()).isEqualTo(90);
    }

    @Test
    void toRestoreRequestSetsIdEnvironmentScopeAndCopiesFields() {
        MaintenanceWindowScheduleResponse source = maintenanceWindowScheduleResponse();
        source.setId(42L);

        MaintenanceWindowScheduleRestoreRequest result = EnvironmentMaintenanceWindowScheduleMapper.toRestoreRequest(source, ENV_CRN);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getScopeType()).isEqualTo(MaintenanceScopeType.ENVIRONMENT.name());
        assertThat(result.getScopeId()).isEqualTo(ENV_CRN);
        assertThat(result.getName()).isEqualTo("nightly");
        assertThat(result.getRecurrenceKind()).isEqualTo(MaintenanceRecurrenceKind.WEEKLY.name());
        assertThat(result.getDurationMinutes()).isEqualTo(90);
        assertThat(result.getStartLocalTime()).isEqualTo("01:30");
        assertThat(result.getDayOfWeek()).isEqualTo("SATURDAY");
    }

    @Test
    void toRestoreRequestPreservesNullNullableFields() {
        MaintenanceWindowScheduleResponse source = maintenanceWindowScheduleResponse();
        source.setId(42L);
        source.setDescription(null);

        MaintenanceWindowScheduleRestoreRequest result = EnvironmentMaintenanceWindowScheduleMapper.toRestoreRequest(source, ENV_CRN);

        assertThat(result.getDescription()).isNull();
    }

    @Test
    void toEnvironmentResponseCopiesScheduleFields() {
        MaintenanceWindowScheduleResponse source = new MaintenanceWindowScheduleResponse();
        source.setName("nightly");
        source.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        source.setDurationMinutes(90);

        assertThat(EnvironmentMaintenanceWindowScheduleMapper.toEnvironmentResponse(source).getName()).isEqualTo("nightly");
        assertThat(EnvironmentMaintenanceWindowScheduleMapper.toEnvironmentResponse(source).getDurationMinutes()).isEqualTo(90);
    }

    private static MaintenanceWindowScheduleResponse maintenanceWindowScheduleResponse() {
        MaintenanceWindowScheduleResponse source = new MaintenanceWindowScheduleResponse();
        source.setName("nightly");
        source.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        source.setDurationMinutes(90);
        source.setStartLocalTime("01:30");
        source.setDayOfWeek("SATURDAY");
        return source;
    }
}
