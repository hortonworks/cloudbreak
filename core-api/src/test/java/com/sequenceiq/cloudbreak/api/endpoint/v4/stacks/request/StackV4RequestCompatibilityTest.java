package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Set;

import jakarta.validation.Configuration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

class StackV4RequestCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4RequestCompatibilityTest.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private Validator validator;

    @BeforeEach
    void setUp() {
        Configuration<?> cfg = Validation.byDefaultProvider().configure();
        cfg.messageInterpolator(new ParameterMessageInterpolator());
        validator = cfg.buildValidatorFactory().getValidator();
    }

    @Test
    void testApiCompatibility2dot4() throws IOException {
        StackV4Request request = mapper.readValue(new ClassPathResource("api-compatibility/StackV4Request_v2.10.0.json").getURL(), StackV4Request.class);
        Set<ConstraintViolation<StackV4Request>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            LOGGER.warn("violations: {}", violations);
        }
        assertEquals(0L, violations.size());
    }
}
