package com.sequenceiq.cloudbreak.controller.validation.stack;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;

public class StackRequestValidatorTestBase {

    private final Logger logger;

    public StackRequestValidatorTestBase(@Nonnull Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    protected void assertValidationErrorIsEmpty(List<String> errors) {
        try {
            assertTrue(errors.isEmpty());
        } catch (AssertionError error) {
            String startMessageFormat = "There should not be any StackRequest validation error but there %s!";
            logger.error(errors.size() > 1 ? format(startMessageFormat, "are") : format(startMessageFormat, "is"));
            errors.forEach(logger::error);
            throw error;
        }
    }

}
