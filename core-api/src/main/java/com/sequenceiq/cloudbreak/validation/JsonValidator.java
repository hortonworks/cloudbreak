package com.sequenceiq.cloudbreak.validation;

import java.io.IOException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonValidator implements ConstraintValidator<ValidJson, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonValidator.class);

    private ObjectMapper objectMapper;

    @Override
    public void initialize(ValidJson constraintAnnotation) {
        objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        try {
            objectMapper.readTree(value);
            return true;
        } catch (IOException ignored) {
            LOGGER.info("exception happened {}", ignored);
        }
        return false;
    }
}