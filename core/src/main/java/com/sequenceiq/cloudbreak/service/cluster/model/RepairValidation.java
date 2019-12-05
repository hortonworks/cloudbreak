package com.sequenceiq.cloudbreak.service.cluster.model;

import java.util.List;
import java.util.Objects;

public class RepairValidation {

    private final List<String> validationErrors;

    public RepairValidation(List<String> validationErrors) {
        this.validationErrors = Objects.requireNonNull(validationErrors);
    }

    public static RepairValidation of(String validationError) {
        return new RepairValidation(List.of(validationError));
    }

    public static RepairValidation of(List<String> validationErrors) {
        return new RepairValidation(validationErrors);
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
