package com.sequenceiq.environment.api.v1.credential.model.parameters.aws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sequenceiq.cloudbreak.util.PasswordUtil;

class RoleBasedParametersRoleArnLengthTest {

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Test
    void testNameValidationWhenNameIsTooShort() {
        String tooShortRoleArn = "tooShortArn";
        RoleBasedParameters request = getRoleBasedParametersRequest(tooShortRoleArn);
        Set<ConstraintViolation<RoleBasedParameters>> constraintViolations = localValidatorFactory.validate(request);
        assertTrue(constraintViolations
                .stream()
                .anyMatch(cv ->
                        RoleBasedParameters.ROLE_ARN_LENGTH_VALIDATION_ERROR_MSG.equals(cv.getMessage())
                ));
    }

    @Test
    void testNameValidationWhenNameIsTooLong() {
        String tooLongRoleArn = PasswordUtil.getRandomAlphabetic(2049);
        RoleBasedParameters request = getRoleBasedParametersRequest(tooLongRoleArn);
        Set<ConstraintViolation<RoleBasedParameters>> constraintViolations = localValidatorFactory.validate(request);
        assertTrue(constraintViolations
                .stream()
                .anyMatch(cv ->
                        RoleBasedParameters.ROLE_ARN_LENGTH_VALIDATION_ERROR_MSG.equals(cv.getMessage())
                ));
    }

    @Test
    void testNameValidationWhenNameIsValidWithBasicLength() {
        String basicRoleArn = "arn:aws-us-gov:iam::000000000000:role/cdpe2e-basic";
        RoleBasedParameters request = getRoleBasedParametersRequest(basicRoleArn);
        Set<ConstraintViolation<RoleBasedParameters>> constraintViolations = localValidatorFactory.validate(request);
        assertFalse(constraintViolations
                .stream()
                .anyMatch(cv ->
                        RoleBasedParameters.ROLE_ARN_LENGTH_VALIDATION_ERROR_MSG.equals(cv.getMessage())
                ));
    }

    @Test
    void testNameValidationWhenNameIsValidWithLengthOver100chars() {
        String roleArnOver100Chars = "arn:aws-us-gov:iam::000000000000:role/cdpe2e-xaccountaccess-stage-xaccountaccess-fxaccountaccess-xaccountaccess";
        RoleBasedParameters request = getRoleBasedParametersRequest(roleArnOver100Chars);
        Set<ConstraintViolation<RoleBasedParameters>> constraintViolations = localValidatorFactory.validate(request);
        assertFalse(constraintViolations
                .stream()
                .anyMatch(cv ->
                        RoleBasedParameters.ROLE_ARN_LENGTH_VALIDATION_ERROR_MSG.equals(cv.getMessage())
                ));
    }

    private RoleBasedParameters getRoleBasedParametersRequest(String roleArn) {
        RoleBasedParameters request = new RoleBasedParameters();
        request.setRoleArn(roleArn);
        return request;
    }
}
