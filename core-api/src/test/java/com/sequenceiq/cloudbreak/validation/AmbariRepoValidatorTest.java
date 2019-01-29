package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;

public class AmbariRepoValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    private AmbariRepoValidator ambariRepoValidator = new AmbariRepoValidator();

    @Test
    public void testAmbari24() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.4");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari25() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.5");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari26() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.6");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari27() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.7");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari25WithMinor() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.5.2");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari24WithMinor() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.4.2");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari26WithMinor() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.6.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari27WithMinor() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("2.7.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari3() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("3");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari30() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("3.0");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari30WithMinor() {
        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();
        ambariRepoDetailsJson.setVersion("3.0.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }
}