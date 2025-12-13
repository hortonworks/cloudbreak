package com.sequenceiq.cloudbreak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;

import jakarta.validation.ConstraintViolation;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;

class BlueprintRequestTest {

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private BlueprintV4Request underTest;

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    void setUp() {
        underTest = new BlueprintV4Request();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Data Lake: Apache Ranger, Apache Atlas, Apache Hive Metastore", 0},
                {"Some-Passw0rd", 0},
                {"doin'n some blueprint?!", 0},
                {"僕だけがいない街", 0},
                {"ਕੁਝ ਨਾਮ ਮੁੱਲ", 0},
                {"အခြို့သောအမညျကိုတနျဖိုး", 0},
                {"некоја вредност за името", 0},
                {"שם ערך כלשהו ?!", 0},
                {"SomePassword", 0},
                {"@#$|:&* ABC", 0},
                {"123456", 0},
                {"", 1},
                {"@#$%|:&*; ABC", 1},
                {"somevalue%12", 1},
                {"somevalue;12", 1},
                {"somevalue/12", 1},
                {"somevalue/", 1},
                {"/", 1},
                {StringUtils.repeat('a', 101), 1}
        });
    }

    @MethodSource("data")
    @ParameterizedTest
    void testBlueprintName(String name, long expectedViolationAmount) {
        underTest.setName(name);
        Set<ConstraintViolation<BlueprintV4Request>> constraintViolations = localValidatorFactory.validate(underTest);
        assertEquals(expectedViolationAmount, countViolationsExceptSpecificOne(constraintViolations));
    }

    private long countViolationsExceptSpecificOne(Set<ConstraintViolation<BlueprintV4Request>> constraintViolations) {
        return constraintViolations.stream().filter(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())).count();
    }

}
