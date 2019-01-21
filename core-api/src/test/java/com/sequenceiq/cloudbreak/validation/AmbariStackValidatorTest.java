package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;

public class AmbariStackValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    private final AmbariStackValidator ambariStackValidator = new AmbariStackValidator();

    @Test
    public void testHdp22() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp23() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.3");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp24() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.4");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp25() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.5");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp26() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.6");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp27() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.7");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp22WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.2.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp23WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.3.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp24WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.4.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp25WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.5.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp26WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.6.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp27WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("2.7.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp3() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp30() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdfWithMajor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDF");
        ambariStackDetailsJson.setVersion("1");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }
}