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

public class MutuallyExclusiveNotNullValidatorTest {

    private static Validator validator;

    @BeforeClass
    public static void setUpClass() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void passSimple1() {
        DummySimpleClass dummyObject = new DummySimpleClass("a", null);
        Set<ConstraintViolation<DummySimpleClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void passSimple2() {
        DummySimpleClass dummyObject = new DummySimpleClass(null, "b");
        Set<ConstraintViolation<DummySimpleClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void failSimpleBothNull() {
        DummySimpleClass dummyObject = new DummySimpleClass(null, null);
        Set<ConstraintViolation<DummySimpleClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failSimpleBothNonNull() {
        DummySimpleClass dummyObject = new DummySimpleClass("a", "b");
        Set<ConstraintViolation<DummySimpleClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void passComplex1() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", "b", null, null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void passComplex2() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, null, "c", "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void failComplexAllNull() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, null, null, null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failComplexAllNonNull() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", "b", "c", "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed1SecondNullGroup() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, "b", null, null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed2SecondNullGroup() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", null, null, null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed1SecondNonNullGroup() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, "b", "c", "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed1SecondMixed1Group() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, "b", null, "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed1SecondMixed2Group() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, "b", "c", null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed2SecondNonNullGroup() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", null, "c", "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed2SecondMixed1Group() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", null, null, "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstMixed2SecondMixed2Group() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", null, "c", null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstNullSecondMixed1Group() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, null, "c", null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstNullSecondMixed2Group() {
        DummyComplexClass dummyObject = new DummyComplexClass(null, null, null, "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstNonNullSecondMixed1Group() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", "b", null, "d");
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void failFirstNonNullSecondMixed2Group() {
        DummyComplexClass dummyObject = new DummyComplexClass("a", "b", "c", null);
        Set<ConstraintViolation<DummyComplexClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void passSemi() {
        DummySemiClass dummyObject = new DummySemiClass("a", "b");
        Set<ConstraintViolation<DummySemiClass>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void failNullSemi() {
        DummySemiClass dummyObject = new DummySemiClass(null, null);
        Set<ConstraintViolation<DummySemiClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void fail1Semi() {
        DummySemiClass dummyObject = new DummySemiClass("a", null);
        Set<ConstraintViolation<DummySemiClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void fail2Semi() {
        DummySemiClass dummyObject = new DummySemiClass(null, "b");
        Set<ConstraintViolation<DummySemiClass>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void passComplexAllowNullWhenAllGroupsNull() {
        DummyClassWithComplexValidationAllowAllGroupsNull dummyObject = new DummyClassWithComplexValidationAllowAllGroupsNull(null, null, null, null);
        Set<ConstraintViolation<DummyClassWithComplexValidationAllowAllGroupsNull>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void failComplexAllowNullWhenAllGroupsFilled() {
        DummyClassWithComplexValidationAllowAllGroupsNull dummyObject = new DummyClassWithComplexValidationAllowAllGroupsNull("a", "b", "c", "d");
        Set<ConstraintViolation<DummyClassWithComplexValidationAllowAllGroupsNull>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void passComplexAllowNullWhenOneGroupIsFilled() {
        DummyClassWithComplexValidationAllowAllGroupsNull dummyObject = new DummyClassWithComplexValidationAllowAllGroupsNull(null, null, "c", "d");
        Set<ConstraintViolation<DummyClassWithComplexValidationAllowAllGroupsNull>> violations = validator.validate(dummyObject);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void failComplexAllowNullWhenOneGroupPartiallyFilled() {
        DummyClassWithComplexValidationAllowAllGroupsNull dummyObject = new DummyClassWithComplexValidationAllowAllGroupsNull("a", null, "c", "d");
        Set<ConstraintViolation<DummyClassWithComplexValidationAllowAllGroupsNull>> violations = validator.validate(dummyObject);
        Assert.assertFalse(violations.isEmpty());
    }

    @MutuallyExclusiveNotNull(fieldGroups = {"a", "b"})
    static class DummySimpleClass {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String a;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String b;

        DummySimpleClass(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    @MutuallyExclusiveNotNull(fieldGroups = {"a,b", "c,d"})
    static class DummyComplexClass {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String a;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String b;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String c;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String d;

        DummyComplexClass(String a, String b, String c, String d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    @MutuallyExclusiveNotNull(fieldGroups = "a,b")
    static class DummySemiClass {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String a;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String b;

        DummySemiClass(String a, String b) {
            this.a = a;
            this.b = b;
        }

    }

    @MutuallyExclusiveNotNull(fieldGroups = {"a,b", "c,d"}, allowAllGroupsNull = true)
    static class DummyClassWithComplexValidationAllowAllGroupsNull {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String a;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String b;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String c;

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        private String d;

        DummyClassWithComplexValidationAllowAllGroupsNull(String a, String b, String c, String d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

}








