package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ChoiceValidatorTest {

    private static Validator validator;

    @BeforeAll
    public static void setUpClass() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void passSimple1() {
        DummyClass dummyObject = new DummyClass(1);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void passSimple2() {
        DummyClass dummyObject = new DummyClass(2);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void failSimple3() {
        DummyClass dummyObject = new DummyClass(3);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        assertFalse(violations.isEmpty());
    }

    static class DummyClass {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        @Choice(intValues = {1, 2})
        private int a;

        DummyClass(int a) {
            this.a = a;
        }
    }
}








