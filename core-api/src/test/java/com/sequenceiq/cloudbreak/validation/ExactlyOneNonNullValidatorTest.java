package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExactlyOneNonNullValidatorTest {

    private ExactlyOneNonNullValidator underTest;

    private TestObject object;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Mock
    private ValidIfExactlyOneNonNull constraint;

    @BeforeEach
    void setUp() {
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new ExactlyOneNonNullValidator();

        lenient().when(constraint.fields()).thenReturn(new String[] { "field1", "field2" });
    }

    @Test
    void testValidFirst() {
        object = new TestObject("value1", null);
        underTest.initialize(constraint);

        assertTrue(underTest.isValid(object, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testValidLast() {
        object = new TestObject(null, "value2");
        underTest.initialize(constraint);

        assertTrue(underTest.isValid(object, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testInvalidMoreThanOne() {
        object = new TestObject("value1", "value2");
        underTest.initialize(constraint);

        assertFalse(underTest.isValid(object, context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testInvalidNone() {
        object = new TestObject(null, null);
        underTest.initialize(constraint);

        assertFalse(underTest.isValid(object, context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testNullIsValid() {
        assertTrue(underTest.isValid(null, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testFieldNotFoundFails() {
        underTest = new ExactlyOneNonNullValidator();

        when(constraint.fields()).thenReturn(new String[] { "field1", "field3" });

        object = new TestObject("value1", null);
        underTest.initialize(constraint);

        assertThrows(IllegalStateException.class, () -> underTest.isValid(object, context));
    }

    private static class TestObject {

        public String field1;

        private String field2;

        TestObject(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }
}
