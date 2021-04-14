package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ValidEnvironmentNameValidatorTest {

    @InjectMocks
    private ValidEnvironmentNameValidator underTest;

    @Mock
    private ConstraintValidatorContext context;

    @Test
    public void testEnvironmentNameValidation() {
        assertTrue(underTest.isValid("apple-tree", context));
        assertFalse(underTest.isValid("apple--tree", context));
        assertFalse(underTest.isValid("apple---tree", context));
        assertFalse(underTest.isValid("apple-tree-", context));
        assertFalse(underTest.isValid("-apple-tree", context));
        assertFalse(underTest.isValid("apple-tree--pear", context));
        assertFalse(underTest.isValid("apple-tree---pear", context));
        assertFalse(underTest.isValid("apple-tree-pear-", context));

    }
}