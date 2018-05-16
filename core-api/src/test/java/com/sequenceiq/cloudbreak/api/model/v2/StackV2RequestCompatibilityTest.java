package com.sequenceiq.cloudbreak.api.model.v2;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StackV2RequestCompatibilityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2RequestCompatibilityTest.class);

    private ObjectMapper mapper = new ObjectMapper();

    private Validator validator;

    @Before
    public void setUp() throws Exception {
        Configuration<?> cfg = Validation.byDefaultProvider().configure();
        cfg.messageInterpolator(new ParameterMessageInterpolator());
        validator = cfg.buildValidatorFactory().getValidator();
    }

    @Test
    public void testApiCompatibility2dot4() throws IOException {
        StackV2Request request = mapper.readValue(new ClassPathResource("api-compatibility/StackV2Request_v2.4.0.json").getURL(), StackV2Request.class);
        Set<ConstraintViolation<StackV2Request>> violations = validator.validate(request);
        if (violations.size() > 0) {
            LOGGER.warn("violations: {}", violations);
        }
        assertEquals(0, violations.size());
    }
}