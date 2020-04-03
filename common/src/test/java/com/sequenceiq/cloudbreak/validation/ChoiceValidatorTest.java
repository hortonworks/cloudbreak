package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ChoiceValidatorTest {

    private static Validator validator;

    @BeforeClass
    public static void setUpClass() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void passSimple1() {
        DummyClass dummyObject = new DummyClass(1);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void passSimple2() {
        DummyClass dummyObject = new DummyClass(2);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void failSimple3() {
        DummyClass dummyObject = new DummyClass(3);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
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








