package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;

import jakarta.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ClusterV4RequestTest {

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private LocalValidatorFactoryBean localValidatorFactory;

    private ClusterV4Request underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClusterV4Request();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Test
    void testClusterRequestCreationWhenNameHasMeetsTheRequirementsThenEverythingGoesFine() {
        underTest.setName("some-name");
        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);
        assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    void testClusterRequestCreationWhenNameDoesNotContainsHyphenThenEverythingGoesFine() {
        underTest.setName("somename");
        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);
        assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    void rejectsInvalidPassword() {
        underTest.setPassword("x");

        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);

        assertTrue(isTooShortPassword(constraintViolations));
        assertFalse(isMissingLetters(constraintViolations));
        assertTrue(isMissingNumbers(constraintViolations));
    }

    @Test
    void rejectsEmptyPassword() {
        underTest.setPassword("");

        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);

        assertTrue(isTooShortPassword(constraintViolations));
        assertTrue(isMissingLetters(constraintViolations));
        assertTrue(isMissingNumbers(constraintViolations));
    }

    @Test
    void rejectsTooShortPassword() {
        underTest.setPassword("asdf123");

        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);

        assertTrue(isTooShortPassword(constraintViolations));
        assertFalse(isMissingLetters(constraintViolations));
        assertFalse(isMissingNumbers(constraintViolations));
    }

    @Test
    void rejectsPasswordWithoutNumber() {
        underTest.setPassword("asdfasdf");

        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);

        assertFalse(isTooShortPassword(constraintViolations));
        assertFalse(isMissingLetters(constraintViolations));
        assertTrue(isMissingNumbers(constraintViolations));
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        underTest.setPassword("12345678");

        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);

        assertFalse(isTooShortPassword(constraintViolations));
        assertTrue(isMissingLetters(constraintViolations));
        assertFalse(isMissingNumbers(constraintViolations));
    }

    @Test
    void acceptsOKPassword() {
        underTest.setPassword("minimum8");

        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);

        assertFalse(isTooShortPassword(constraintViolations));
        assertFalse(isMissingLetters(constraintViolations));
        assertFalse(isMissingNumbers(constraintViolations));
    }

    private boolean isTooShortPassword(Collection<ConstraintViolation<ClusterV4Request>> constraintViolations) {
        return constraintViolations.stream().anyMatch(violation -> violation.getMessage().contains("length of the password"));
    }

    private boolean isMissingLetters(Collection<ConstraintViolation<ClusterV4Request>> constraintViolations) {
        return constraintViolations.stream().anyMatch(violation -> violation.getMessage().contains("password should contain at least one letter"));
    }

    private boolean isMissingNumbers(Collection<ConstraintViolation<ClusterV4Request>> constraintViolations) {
        return constraintViolations.stream().anyMatch(violation -> violation.getMessage().contains("password should contain at least one number"));
    }

}
