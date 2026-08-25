package com.sequenceiq.maintenance.api.v1.task.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class UpdateMaintenanceWindowTaskRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void rejectsMissingVersion() {
        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setStatus("DISABLED");

        Set<ConstraintViolation<UpdateMaintenanceWindowTaskRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "version".equals(v.getPropertyPath().toString()));
    }

    @Test
    void acceptsDisableWithVersion() {
        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setVersion(1);
        request.setStatus("DISABLED");

        assertThat(validator.validate(request)).isEmpty();
    }
}
