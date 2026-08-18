package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MaintenanceWindowScheduleListParamsTest {

    private final Validator validator = validator();

    @Test
    void acceptsBothScopeParamsOmitted() {
        assertTrue(validator.validate(new MaintenanceWindowScheduleListParams()).isEmpty());
    }

    @Test
    void acceptsBothScopeParamsProvided() {
        MaintenanceWindowScheduleListParams params = new MaintenanceWindowScheduleListParams();
        params.setScopeType("TENANT");
        params.setScopeId("acc-1");
        assertTrue(validator.validate(params).isEmpty());
    }

    @Test
    void rejectsScopeIdWithoutScopeType() {
        MaintenanceWindowScheduleListParams params = new MaintenanceWindowScheduleListParams();
        params.setScopeId("acc-1");
        Set<ConstraintViolation<MaintenanceWindowScheduleListParams>> violations = validator.validate(params);
        assertFalse(violations.isEmpty());
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("scopeType and scopeId must both be provided or both omitted");
    }

    @Test
    void rejectsScopeTypeWithoutScopeId() {
        MaintenanceWindowScheduleListParams params = new MaintenanceWindowScheduleListParams();
        params.setScopeType("TENANT");
        Set<ConstraintViolation<MaintenanceWindowScheduleListParams>> violations = validator.validate(params);
        assertFalse(violations.isEmpty());
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("scopeType and scopeId must both be provided or both omitted");
    }
}
