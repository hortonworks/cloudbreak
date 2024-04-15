package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

public class ValidationTestUtil {

    private ValidationTestUtil() {
    }

    public static void checkErrorsPresent(ValidationResult.ValidationResultBuilder resultBuilder, List<String> errorMessages) {
        ValidationResult validationResult = resultBuilder.build();
        assertEquals(errorMessages.size(), validationResult.getErrors().size(), validationResult.getFormattedErrors());
        List<String> actual = validationResult.getErrors();
        errorMessages.forEach(message -> assertTrue(actual.stream().anyMatch(item -> item.equals(message)), validationResult::getFormattedErrors));
    }

    public static void checkWarningsPresent(ValidationResult.ValidationResultBuilder resultBuilder, List<String> warningMessages) {
        ValidationResult validationResult = resultBuilder.build();
        assertEquals(warningMessages.size(), validationResult.getWarnings().size(), validationResult.getFormattedWarnings());
        List<String> actual = validationResult.getWarnings();
        warningMessages.forEach(message -> assertTrue(actual.stream().anyMatch(item -> item.equals(message)), validationResult::getFormattedWarnings));
    }

}