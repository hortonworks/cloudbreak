package com.sequenceiq.maintenance.api.v1.task.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MaintenanceWindowTaskRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsValidRequest() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsMissingExecutionRef() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setExecutionRef(null);

        Set<ConstraintViolation<MaintenanceWindowTaskRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "executionRef".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidTaskKind() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setTaskKind("ALWAYS");

        Set<ConstraintViolation<MaintenanceWindowTaskRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("taskKind"));
    }

    @Test
    void rejectsIncompleteDependsOn() {
        MaintenanceWindowTaskRequest request = validRequest();
        MaintenanceWindowTaskDependencyRequest dependsOn = new MaintenanceWindowTaskDependencyRequest();
        dependsOn.setTaskType("DATABASE_UPGRADE");
        request.setDependsOn(dependsOn);

        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().startsWith("dependsOn"));
    }

    private MaintenanceWindowTaskRequest validRequest() {
        MaintenanceWindowTaskRequest request = new MaintenanceWindowTaskRequest();
        request.setResourceCrn("crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1");
        request.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-1");
        request.setTaskType("secret-rotation");
        request.setWorkItemId("secret-1");
        request.setTaskKind("EVERY_WINDOW");
        request.setSubmitterService("secret-rotation-service");
        request.setExecutionRef(Map.of("type", "http"));
        return request;
    }
}
