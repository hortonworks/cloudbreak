package com.sequenceiq.maintenance.api.v1.task.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MaintenanceWindowTaskListParamsValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsEmptyFilters() {
        assertThat(validator.validate(new MaintenanceWindowTaskListParams())).isEmpty();
    }

    @Test
    void acceptsResourceCrnWithTaskType() {
        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setResourceCrn("crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1");
        params.setTaskType("secret-rotation");
        params.setWorkItemId("secret-1");
        params.setTaskKind("ONE_SHOT");
        params.setStatus("ACTIVE");
        params.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-1");

        assertThat(validator.validate(params)).isEmpty();
    }

    @Test
    void rejectsTaskTypeWithoutResourceCrn() {
        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setTaskType("secret-rotation");

        Set<ConstraintViolation<MaintenanceWindowTaskListParams>> violations = validator.validate(params);

        assertThat(violations).anyMatch(v -> "taskTypeFilterValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsWorkItemIdWithoutTaskTypeAndResourceCrn() {
        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setWorkItemId("secret-1");

        Set<ConstraintViolation<MaintenanceWindowTaskListParams>> violations = validator.validate(params);

        assertThat(violations).anyMatch(v -> "workItemIdFilterValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsWorkItemIdWithoutTaskTypeWhenResourceCrnPresent() {
        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setResourceCrn("crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1");
        params.setWorkItemId("secret-1");

        Set<ConstraintViolation<MaintenanceWindowTaskListParams>> violations = validator.validate(params);

        assertThat(violations).anyMatch(v -> "workItemIdFilterValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidTaskKind() {
        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setTaskKind("ALWAYS");

        Set<ConstraintViolation<MaintenanceWindowTaskListParams>> violations = validator.validate(params);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("taskKind"));
    }

    @Test
    void rejectsInvalidStatus() {
        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setStatus("RUNNING");

        Set<ConstraintViolation<MaintenanceWindowTaskListParams>> violations = validator.validate(params);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("status"));
    }
}
