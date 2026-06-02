package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlueprintV4RequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testNameScriptInjection() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setName("<script>alert(1)</script>");

        Set<ConstraintViolation<BlueprintV4Request>> violations = validator.validateProperty(request, "name");

        assertThat(violations).isNotEmpty();
        Assertions.assertEquals("Resource name cannot contain special characters like <, >, ;, /, \\, or %.", violations.iterator().next().getMessage());
    }

    @Test
    void testDescriptionScriptInjection() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setName("validname");
        request.setDescription("<svg onload=alert(1)/>");

        Set<ConstraintViolation<BlueprintV4Request>> violations = validator.validateProperty(request, "description");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Description cannot contain HTML tags or the < and > characters.");
    }

    @Test
    void testValidRequest() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setName("validname");
        request.setDescription("valid description");

        Set<ConstraintViolation<BlueprintV4Request>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}