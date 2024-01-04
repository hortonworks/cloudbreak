package com.sequenceiq.cloudbreak.controller.validation;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.validation.JsonValidator;
import com.sequenceiq.cloudbreak.validation.ValidJson;

@RunWith(MockitoJUnitRunner.class)
public class JsonValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private JsonValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidJson validJson;

    @Before
    public void setUp() {
        underTest.initialize(validJson);
    }

    @Test
    public void testNullValue() {
        Assert.assertTrue(underTest.isValid(null, constraintValidatorContext));
    }

    @Test
    public void testEmptyValue() {
        Assert.assertTrue(underTest.isValid("", constraintValidatorContext));
    }

    @Test
    public void testJsonValue() {
        Assert.assertTrue(underTest.isValid("{}", constraintValidatorContext));
    }

    @Test
    public void testInvalidJsonValue() {
        Assert.assertFalse(underTest.isValid(".", constraintValidatorContext));
    }
}
