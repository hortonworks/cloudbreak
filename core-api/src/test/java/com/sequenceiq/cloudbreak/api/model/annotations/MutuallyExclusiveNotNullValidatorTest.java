package com.sequenceiq.cloudbreak.api.model.annotations;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MutuallyExclusiveNotNullValidatorTest {

    private static Validator validator;

    @BeforeClass
    public static void setUpClass() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void pass1() {
        DummyClass dummyObject = new DummyClass("a", null);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void pass2() {
        DummyClass dummyObject = new DummyClass(null, "b");
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void fail_both_null() {
        DummyClass dummyObject = new DummyClass(null, null);
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void fail_both_nonnull() {
        DummyClass dummyObject = new DummyClass("a", "b");
        Set<ConstraintViolation<DummyClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @MutuallyExclusiveNotNull(fieldNames = {"a", "b"})
    static class DummyClass {
        String a;
        String b;

        DummyClass(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
}
