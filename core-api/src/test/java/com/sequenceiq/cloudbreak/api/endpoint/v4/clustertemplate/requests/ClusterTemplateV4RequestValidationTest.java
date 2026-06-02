package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;

class ClusterTemplateV4RequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testNameScriptInjection() {
        ClusterTemplateV4Request request = new ClusterTemplateV4Request();
        request.setName("<script>alert(1)</script>");
        request.setDistroXTemplate(new DistroXV1Request());

        Set<ConstraintViolation<ClusterTemplateV4Request>> violations = validator.validateProperty(request, "name");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Resource name cannot contain special characters like <, >, ;, /, \\, or %.");
    }

    @Test
    void testDescriptionScriptInjection() {
        ClusterTemplateV4Request request = new ClusterTemplateV4Request();
        request.setName("validname");
        request.setDescription("<svg onload=alert(1)/>");
        request.setDistroXTemplate(new DistroXV1Request());

        Set<ConstraintViolation<ClusterTemplateV4Request>> violations = validator.validateProperty(request, "description");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Description cannot contain HTML tags or the < and > characters.");
    }

    @Test
    void testValidRequest() {
        ClusterTemplateV4Request request = new ClusterTemplateV4Request();
        request.setName("validname");
        request.setDescription("valid description");
        request.setDistroXTemplate(new DistroXV1Request());

        Set<ConstraintViolation<ClusterTemplateV4Request>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
