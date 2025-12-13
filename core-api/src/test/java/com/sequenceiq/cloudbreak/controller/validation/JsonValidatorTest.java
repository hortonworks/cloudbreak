package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.JsonValidator;
import com.sequenceiq.cloudbreak.validation.ValidJson;

@ExtendWith(MockitoExtension.class)
class JsonValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private JsonValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidJson validJson;

    @BeforeEach
    void setUp() {
        underTest.initialize(validJson);
    }

    @Test
    void testNullValue() {
        assertTrue(underTest.isValid(null, constraintValidatorContext));
    }

    @Test
    void testEmptyValue() {
        assertTrue(underTest.isValid("", constraintValidatorContext));
    }

    @Test
    void testJsonValue() {
        assertTrue(underTest.isValid("{}", constraintValidatorContext));
    }

    @Test
    void testInvalidJsonValue() {
        assertFalse(underTest.isValid(".", constraintValidatorContext));
    }
}
