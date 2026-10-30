package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustBase.INVALID_TRUST_SECRET_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.AddCrossRealmTrustV2Request;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2Request;

class TrustSecretValidationTest {

    private static final String TRUST_SECRET = "trustSecret";

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    static Stream<Arguments> trustSecretDataProvider() {
        Stream<Function<String, Object>> requestFactories = Stream.of(
                TrustSecretValidationTest::prepareCrossRealmTrustRequest,
                TrustSecretValidationTest::prepareCrossRealmTrustV2Request,
                TrustSecretValidationTest::addCrossRealmTrustV2Request);
        Object[][] secrets = {
                {null, true},
                {"", true},
                {" ", true},
                {"   ", true},
                {"\t\n", true},
                {"a", true},
                {"Secret123", true},
                {"A-Za-z0-9@#$*+,./:;=?_~-", true},
                {"a".repeat(128), true},
                {"a".repeat(129), false},
                {" secret", false},
                {"secret ", false},
                {"secret with space", false},
                {"secret!", false},
                {"secret\"quote", false},
                {"secret'quote", false},
                {"secret\\backslash", false},
                {"sécret", false},
        };
        return requestFactories.flatMap(factory -> Stream.of(secrets)
                .map(secret -> {
                    Object request = factory.apply((String) secret[0]);
                    return Arguments.of(request.getClass().getSimpleName(), request, secret[0], secret[1]);
                }));
    }

    @ParameterizedTest(name = "{0} with trustSecret [{2}] valid: {3}")
    @MethodSource("trustSecretDataProvider")
    void testTrustSecretValidation(String requestType, Object request, String trustSecret, boolean expectedValid) {
        Set<ConstraintViolation<Object>> violations = validator.validateProperty(request, TRUST_SECRET);

        assertEquals(expectedValid, violations.isEmpty(), "Unexpected violations: " + violations);
        if (!expectedValid) {
            assertTrue(violations.stream().allMatch(violation -> INVALID_TRUST_SECRET_MESSAGE.equals(violation.getMessage())));
        }
    }

    private static PrepareCrossRealmTrustRequest prepareCrossRealmTrustRequest(String trustSecret) {
        PrepareCrossRealmTrustRequest request = new PrepareCrossRealmTrustRequest();
        request.setTrustSecret(trustSecret);
        return request;
    }

    private static PrepareCrossRealmTrustV2Request prepareCrossRealmTrustV2Request(String trustSecret) {
        PrepareCrossRealmTrustV2Request request = new PrepareCrossRealmTrustV2Request();
        request.setTrustSecret(trustSecret);
        return request;
    }

    private static AddCrossRealmTrustV2Request addCrossRealmTrustV2Request(String trustSecret) {
        AddCrossRealmTrustV2Request request = new AddCrossRealmTrustV2Request();
        request.setTrustSecret(trustSecret);
        return request;
    }
}
