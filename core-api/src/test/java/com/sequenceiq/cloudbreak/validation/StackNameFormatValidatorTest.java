package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StackNameFormatValidatorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @InjectMocks
    private StackNameFormatValidator underTest;

    @Test
    public void testValidationSuccess() {
        assertTrue(underTest.isValid("right-format", context));
        assertTrue(underTest.isValid("stack0123", context));
        assertTrue(underTest.isValid("stack0123stack", context));

    }

    @Test
    public void testValidationFailure() {
        assertFalse(underTest.isValid("-stack-invalid", context));
        assertFalse(underTest.isValid("Astack", context));
        assertFalse(underTest.isValid("stack.stack", context));
        assertFalse(underTest.isValid("stack_stack", context));
        assertFalse(underTest.isValid("stack-", context));
        assertFalse(underTest.isValid(null, context));
    }
}
