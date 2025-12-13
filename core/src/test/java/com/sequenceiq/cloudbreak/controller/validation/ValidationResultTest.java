package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

class ValidationResultTest {

    private static final String ERROR_MESSAGE = "Error should appear now.";

    private ValidationResultBuilder underTest;

    @BeforeEach
    public void setup() {
        underTest = ValidationResult.builder();
    }

    @Test
    void testIfErrorWorks() {
        underTest.ifError(() -> true, ERROR_MESSAGE);
        ValidationResult result = underTest.build();
        assertError(result);
    }

    @Test
    void testErrorWorks() {
        underTest.error(ERROR_MESSAGE);
        ValidationResult result = underTest.build();
        assertError(result);
    }

    @Test
    void testDuplicateErrors() {
        underTest.error(ERROR_MESSAGE);
        underTest.error(ERROR_MESSAGE);
        ValidationResult result = underTest.build();
        assertError(result);
    }

    private void assertError(ValidationResult result) {
        assertEquals(State.ERROR, result.getState());
        assertEquals(ERROR_MESSAGE, result.getErrors().get(0));
    }

}
