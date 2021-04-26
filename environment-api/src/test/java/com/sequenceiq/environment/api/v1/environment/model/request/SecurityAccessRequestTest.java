package com.sequenceiq.environment.api.v1.environment.model.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SecurityAccessRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    static Object[][] cidrDataProvider() {
        return new Object[][] {
            { null, false },
            { "", false },
            { "asdf", false },
            { "0.0.0.0", false },
            { "192.168.1.1", false },
            { "192.168.1.1/32", true },
            { "192.268.1.1/33", false },
            { "0.0.0.0/0", true },
            { "0.0.0.0/x", false },
            { "::0", false },
            { "::x", false },
            { "::0/0", true },
            { "::0/x", false },
            { "fe80:0000:0000:0000:0204:61ff:fe9d:f156", false },
            { "fe80:0000:0000:0000:0204:61ff:fe9d:f156/x", false },
            { "fe80:0000:0000:0000:0204:61ff:fe9d:f15x/128", false },
            { "fe80:0000:0000:0000:0204:61ff:fe9d:f156/128", true },
            { "fe80:0000:0000:0000:0204:61ff:fe9d:f156/129", false },
            { "fe80::0204:61ff:fe9d:f156/9", true }
        };
    }

    @ParameterizedTest
    @MethodSource("cidrDataProvider")
    void testCidrValidation(String cidr, boolean expected) {
        SecurityAccessRequest request = SecurityAccessRequest.builder()
                .withCidr(cidr)
                .build();
        Set<ConstraintViolation<SecurityAccessRequest>> violations = validator.validate(request);
        assertEquals(expected, violations.isEmpty());
    }
}
