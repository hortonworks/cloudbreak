package com.sequenceiq.environment.api.v1.environment.model.request;

import static com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest.LENGHT_INVALID_MSG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class EnvironmentRequestNameLengthValidationTest {

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Test
    void testNameValidationWhenNameIsTooShort() {
        String shortEnvName = "alma";
        EnvironmentRequest environmentRequestWithLocation = createEnvironmentRequestWithLocation(shortEnvName);
        Set<ConstraintViolation<EnvironmentRequest>> constraintViolations = localValidatorFactory.validate(environmentRequestWithLocation);
        assertTrue(constraintViolations
                .stream()
                .anyMatch(cv ->
                        LENGHT_INVALID_MSG.equals(cv.getMessage())
                ));
    }

    @Test
    void testNameValidationWhenNameIsInTheValidRange() {
        String validName = "my-valid-environment-name";
        EnvironmentRequest environmentRequestWithLocation = createEnvironmentRequestWithLocation(validName);
        Set<ConstraintViolation<EnvironmentRequest>> constraintViolations = localValidatorFactory.validate(environmentRequestWithLocation);
        assertTrue(constraintViolations.isEmpty());
    }

    @Test
    void testNameValidationWhenNameIsTooLong() {
        String longEnvName = "my-D04CEC63-95AF-4D83-9198-512CB9DD901B";
        EnvironmentRequest environmentRequestWithLocation = createEnvironmentRequestWithLocation(longEnvName);
        Set<ConstraintViolation<EnvironmentRequest>> constraintViolations = localValidatorFactory.validate(environmentRequestWithLocation);
        assertTrue(constraintViolations
                .stream()
                .anyMatch(cv ->
                        LENGHT_INVALID_MSG.equals(cv.getMessage())
                ));
    }

    private EnvironmentRequest createEnvironmentRequestWithLocation(String name) {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(name);
        environmentRequest.setLocation(new LocationRequest());
        return environmentRequest;
    }
}
