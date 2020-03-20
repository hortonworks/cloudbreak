package com.sequenceiq.environment.api.v1.environment.validator;


import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;

class NetworkCidrValidatorTest {

    public static final String EMPTY_STRING = "";

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidatorShouldFailWhenMaskIsNot16() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setNetworkCidr("172.16.0.0/17");
        Set<ConstraintViolation<EnvironmentNetworkRequest>> violation = validator.validate(request);
        MatcherAssert.assertThat(violation, hasSize(1));
        MatcherAssert.assertThat(violation, hasItem(
                hasProperty("message", is("The format of the CIDR is not accepted. Prefix mask must be /16"))));
    }

    @Test
    void testValidatorShouldPassWhenCidrMaskIs16() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setNetworkCidr("172.16.0.0/16");
        Set<ConstraintViolation<EnvironmentNetworkRequest>> violation = validator.validate(request);
        MatcherAssert.assertThat(violation, empty());
    }

    @Test
    void testValidatorShouldPassWhenCidrIsNull() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setSubnetIds(Set.of("subnet-id"));
        Set<ConstraintViolation<EnvironmentNetworkRequest>> violation = validator.validate(request);
        MatcherAssert.assertThat(violation, empty());
    }

    @Test
    void testValidatorShouldPassWhenCidrIsEmptySinceCliCanProduceThat() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setNetworkCidr(EMPTY_STRING);
        Set<ConstraintViolation<EnvironmentNetworkRequest>> violation = validator.validate(request);
        MatcherAssert.assertThat(violation, hasSize(0));
    }

    @Test
    void testValidatorShouldFailWhenCidrIsNotACidr() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setNetworkCidr("16/16");
        Set<ConstraintViolation<EnvironmentNetworkRequest>> violation = validator.validate(request);
        MatcherAssert.assertThat(violation, hasSize(2));
        MatcherAssert.assertThat(violation, hasItem(
                hasProperty("message", is("The format of the CIDR is not accepted. Prefix mask must be /16"))));
        MatcherAssert.assertThat(violation, hasItem(
                hasProperty("message", is("The field should contain a valid CIDR definition."))));
    }

    @Test
    void testValidatorShouldFailWhenCidrIsNotRfcCompliant() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setNetworkCidr("0.0.0.0/16");
        Set<ConstraintViolation<EnvironmentNetworkRequest>> violation = validator.validate(request);
        MatcherAssert.assertThat(violation, hasSize(1));
        MatcherAssert.assertThat(violation, hasItem(
                hasProperty("message", is("The field should contain a valid CIDR definition."))));
    }
}