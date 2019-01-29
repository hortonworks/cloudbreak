package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;

public class AmbariStackValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    private AmbariStackValidator ambariStackValidator = new AmbariStackValidator();

    @Test
    public void testHdp22() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp23() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.3");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp24() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.4");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp25() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.5");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp26() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.6");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp27() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.7");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp22WithMinor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.2.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp23WithMinor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.3.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp24WithMinor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.4.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp25WithMinor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.5.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp26WithMinor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.6.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp27WithMinor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.7.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp3() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp30() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdfWithMajor() {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();
        ambariStackDetailsJson.setStack("HDF");
        ambariStackDetailsJson.setVersion("1");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }
}