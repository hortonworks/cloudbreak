package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;

public class AmbariRepositoryV4ValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    private final AmbariRepositoryV4Validator ambariRepoValidator = new AmbariRepositoryV4Validator();

    @Test
    public void testAmbari24() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.4");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari25() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.5");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari26() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.6");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari27() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.7");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari25WithMinor() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.5.2");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari24WithMinor() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.4.2");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari26WithMinor() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.6.2");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari27WithMinor() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("2.7.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari3() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("3");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari30() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("3.0");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari30WithMinor() {
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        ambariRepoDetailsJson.setVersion("3.0.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }
}