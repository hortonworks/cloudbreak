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
class StackNameLengthValidatorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @InjectMocks
    private StackNameLengthValidator underTest;

    @Test
    void testValidationSuccess() {
        assertTrue(underTest.isValid("right-length", context));
    }

    @Test
    void testValidationFailure() {
        assertFalse(underTest.isValid("asd", context));
        assertFalse(underTest.isValid("thisisaverylongstacknamewhichisnotallowedbecauseitistoolong", context));
        assertFalse(underTest.isValid(null, context));
    }
}
