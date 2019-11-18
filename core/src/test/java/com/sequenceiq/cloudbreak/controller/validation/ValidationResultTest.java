package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

public class ValidationResultTest {

    private static final String ERROR_MESSAGE = "Error should appear now.";

    private ValidationResultBuilder underTest;

    @Before
    public void setup() {
        underTest = ValidationResult.builder();
    }

    @Test
    public void testIfErrorWorks() {
        underTest.ifError(() -> true, ERROR_MESSAGE);
        ValidationResult result = underTest.build();
        assertError(result);
    }

    @Test
    public void testErrorWorks() {
        underTest.error(ERROR_MESSAGE);
        ValidationResult result = underTest.build();
        assertError(result);
    }

    @Test
    public void testDuplicateErrors() {
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
