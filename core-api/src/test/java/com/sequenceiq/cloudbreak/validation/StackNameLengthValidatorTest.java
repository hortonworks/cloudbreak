package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StackNameLengthValidatorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @InjectMocks
    private StackNameLengthValidator underTest;

    @Test
    public void testValidationSuccess() {
        assertTrue(underTest.isValid("right-length", context));
    }

    @Test
    public void testValidationFailure() {
        assertFalse(underTest.isValid("asd", context));
        assertFalse(underTest.isValid("thisisaverylongstacknamewhichisnotallowedbecauseitistoolong", context));
        assertFalse(underTest.isValid(null, context));
    }
}
