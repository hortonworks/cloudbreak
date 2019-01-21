package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;

public class AmbariRepoValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    private final AmbariRepoValidator ambariRepoValidator = new AmbariRepoValidator();

    @Test
    public void testAmbari24() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.4");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari25() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.5");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari26() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.6");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari27() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.7");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari25WithMinor() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.5.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari24WithMinor() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.4.2");
        assertFalse(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari26WithMinor() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.6.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari27WithMinor() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("2.7.2");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari3() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("3");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testAmbari30() {
        AmbariRepositoryV4Response ambariRepoDetailsJson = new AmbariRepositoryV4Response();
        ambariRepoDetailsJson.setVersion("3.0");
        assertTrue(ambariRepoValidator.isValid(ambariRepoDetailsJson, constraintValidatorContext));
    }
}