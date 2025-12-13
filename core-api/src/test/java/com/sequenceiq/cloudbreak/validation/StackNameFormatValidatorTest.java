package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StackNameFormatValidatorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @InjectMocks
    private StackNameFormatValidator underTest;

    @Test
    void testValidationSuccess() {
        assertTrue(underTest.isValid("right-format", context));
        assertTrue(underTest.isValid("stack0123", context));
        assertTrue(underTest.isValid("stack0123stack", context));

    }

    @Test
    void testValidationFailure() {
        assertFalse(underTest.isValid("-stack-invalid", context));
        assertFalse(underTest.isValid("Astack", context));
        assertFalse(underTest.isValid("stack.stack", context));
        assertFalse(underTest.isValid("stack_stack", context));
        assertFalse(underTest.isValid("stack-", context));
        assertFalse(underTest.isValid(null, context));
    }
}
