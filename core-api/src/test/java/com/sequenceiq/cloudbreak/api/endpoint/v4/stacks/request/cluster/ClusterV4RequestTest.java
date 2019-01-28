package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class ClusterV4RequestTest {

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private static final long EXPECTED_VIOLATION_AMOUNT = 1L;

    private LocalValidatorFactoryBean localValidatorFactory;

    private ClusterV4Request underTest;

    @Before
    public void setUp() {
        underTest = new ClusterV4Request();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Test
    public void testClusterRequestCreationWhenNameHasMeetsTheRequirementsThenEverythingGoesFine() {
        underTest.setName("some-name");
        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenNameDoesNotContainsHyphenThenEverythingGoesFine() {
        underTest.setName("somename");
        Set<ConstraintViolation<ClusterV4Request>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    private long countViolationsExceptSpecificOne(Set<ConstraintViolation<ClusterV4Request>> constraintViolations) {
        return constraintViolations.stream().filter(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())).count();
    }

}