package com.sequenceiq.maintenance.api.v1.task.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class UpdateMaintenanceWindowTaskRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsDisableWithoutVersion() {
        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setStatus("DISABLED");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsEmptyUpdate() {
        assertThat(validator.validate(new UpdateMaintenanceWindowTaskRequest())).isEmpty();
    }
}
