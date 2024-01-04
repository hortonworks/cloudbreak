package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ExactlyOneNonNullValidatorTest {

    private ExactlyOneNonNullValidator underTest;

    private TestObject object;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Mock
    private ValidIfExactlyOneNonNull constraint;

    @Before
    public void setUp() {
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new ExactlyOneNonNullValidator();

        when(constraint.fields()).thenReturn(new String[] { "field1", "field2" });
    }

    @Test
    public void testValidFirst() {
        object = new TestObject("value1", null);
        underTest.initialize(constraint);

        assertTrue(underTest.isValid(object, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testValidLast() {
        object = new TestObject(null, "value2");
        underTest.initialize(constraint);

        assertTrue(underTest.isValid(object, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testInvalidMoreThanOne() {
        object = new TestObject("value1", "value2");
        underTest.initialize(constraint);

        assertFalse(underTest.isValid(object, context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testInvalidNone() {
        object = new TestObject(null, null);
        underTest.initialize(constraint);

        assertFalse(underTest.isValid(object, context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testNullIsValid() {
        assertTrue(underTest.isValid(null, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNotFoundFails() {
        underTest = new ExactlyOneNonNullValidator();

        when(constraint.fields()).thenReturn(new String[] { "field1", "field3" });

        object = new TestObject("value1", null);
        underTest.initialize(constraint);

        underTest.isValid(object, context);
    }

    @SuppressFBWarnings(value = "UrF", justification = "This is just a test class")
    private static class TestObject {

        public String field1;

        private String field2;

        TestObject(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }
}
