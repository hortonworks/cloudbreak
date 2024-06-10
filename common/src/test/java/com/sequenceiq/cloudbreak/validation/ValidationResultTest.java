package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ValidationResultTest {

    @Test
    public void testEmptyValidationResult() {
        ValidationResult result = ValidationResult.empty();
        assertEquals(ValidationResult.State.VALID, result.getState());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testAddingErrorChangesStateToError() {
        ValidationResult result = ValidationResult.builder().error("Sample error").build();
        assertEquals(ValidationResult.State.ERROR, result.getState());
        assertTrue(result.getErrors().contains("Sample error"));
    }

    @Test
    public void testAddingWarningDoesNotChangeState() {
        ValidationResult result = ValidationResult.builder().warning("Sample warning").build();
        assertEquals(ValidationResult.State.VALID, result.getState());
        assertTrue(result.getWarnings().contains("Sample warning"));
    }

    @Test
    public void testMergingValidationResultsCombinesErrorsAndWarnings() {
        ValidationResult result1 = ValidationResult.builder().error("Error 1").warning("Warning 1").build();
        ValidationResult result2 = ValidationResult.builder().error("Error 2").warning("Warning 2").build();
        ValidationResult mergedResult = result1.merge(result2);
        assertTrue(mergedResult.getErrors().containsAll(List.of("Error 1", "Error 2")));
        assertTrue(mergedResult.getWarnings().containsAll(List.of("Warning 1", "Warning 2")));
    }

    @Test
    public void testFormattingProducesNumberedListForMultipleItems() {
        ValidationResult result = ValidationResult.builder().error("Error 1").error("Error 2").build();
        String formattedErrors = result.getFormattedErrors();
        assertTrue(formattedErrors.contains("1. Error 1"));
        assertTrue(formattedErrors.contains("2. Error 2"));
    }

    @Test
    public void testBuilderSetsPrefixCorrectly() {
        ValidationResult result = ValidationResult.builder().prefix("Prefix").error("Error").error("asd").warning("Warning").build();
        assertTrue(result.getFormattedErrors().startsWith("Prefix"));
        assertTrue(result.getFormattedWarnings().startsWith("Prefix"));
    }

    @Test
    public void testMergingValidWithErrorResultsInErrorState() {
        ValidationResult validResult = ValidationResult.empty();
        ValidationResult errorResult = ValidationResult.builder().error("Error").build();
        ValidationResult mergedResult = validResult.merge(errorResult);
        assertEquals(ValidationResult.State.ERROR, mergedResult.getState());
    }

    @Test
    public void testAddingEmptyErrorOrWarningDoesNotAffectStateOrLists() {
        ValidationResult result = ValidationResult.builder().error("").warning("").build();
        assertEquals(ValidationResult.State.VALID, result.getState());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testMergingWithNullDoesNotAlterOriginal() {
        ValidationResult original = ValidationResult.builder().error("Error").build();
        ValidationResult merged = original.merge(null);
        assertEquals(original.getState(), merged.getState());
        assertEquals(original.getErrors(), merged.getErrors());
        assertEquals(original.getWarnings(), merged.getWarnings());
    }

    @Test
    public void testFormattingSingleItemDoesNotIncludeNumbering() {
        ValidationResult result = ValidationResult.builder().error("Single Error").build();
        String formattedErrors = result.getFormattedErrors();
        assertFalse(formattedErrors.contains("1. "));
        assertTrue(formattedErrors.contains("Single Error"));
    }

    @Test
    public void testPrefixHandledCorrectlyWhenEmptyOrNull() {
        ValidationResult resultWithEmptyPrefix = ValidationResult.builder().prefix("").error("Error").build();
        assertFalse(resultWithEmptyPrefix.getFormattedErrors().startsWith(":"));

        ValidationResult resultWithNullPrefix = ValidationResult.builder().prefix(null).error("Error").build();
        assertFalse(resultWithNullPrefix.getFormattedErrors().startsWith(":"));
    }

    @Test
    public void testHasErrorReturnsTrueIfThereAreErrors() {
        ValidationResult result = ValidationResult.builder().error("Error").build();
        assertTrue(result.hasError());
    }

    @Test
    public void testHasWarningReturnsTrueIfThereAreWarnings() {
        ValidationResult validationResult = ValidationResult.builder().warning("Warning 1").build();
        assertTrue(validationResult.hasWarning());
    }

    @Test
    public void testBuilderMultipleErrorsWarnings() {
        ValidationResult validationResult = ValidationResult.builder()
                .error("Error 1")
                .warning("Warning 1")
                .merge(ValidationResult.builder().error("Error 2").warning("Warning 2").build())
                .build();

        assertTrue(validationResult.hasError());
        assertTrue(validationResult.hasWarning());
        assertEquals(2, validationResult.getErrors().size());
        assertEquals(2, validationResult.getWarnings().size());
    }
}