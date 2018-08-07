package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;

public class ClusterRequestTest {

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private static final long EXPECTED_VIOLATION_AMOUNT = 1L;

    private LocalValidatorFactoryBean localValidatorFactory;

    private ClusterRequest underTest;

    @Before
    public void setUp() {
        underTest = new ClusterRequest();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Test
    public void testClusterRequestCreationWhenNameHasMeetsTheRequirementsThenEverythingGoesFine() {
        underTest.setName("some-name");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenNameDoesNotContainsHyphenThenEverythingGoesFine() {
        underTest.setName("somename");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenNameStartsWithHyphenThenViolationHappens() {
        underTest.setName("-somename");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenNameEndsWithHyphenThenViolationHappens() {
        underTest.setName("somename-");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenNameContainsOnlyHyphenThenViolationHappens() {
        underTest.setName("------");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenNameContainsAnUpperCaseLetterThenViolationHappens() {
        underTest.setName("somEname");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenUsernameHasMeetsTheRequirementsThenEverythingGoesFine() {
        underTest.setUserName("some-name");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenUsernameDoesNotContainsHyphenThenEverythingGoesFine() {
        underTest.setUserName("somename");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenUsernameStartsWithHyphenThenViolationHappens() {
        underTest.setUserName("-somename");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenUsernameEndsWithHyphenThenViolationHappens() {
        underTest.setUserName("somename-");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenUsernameContainsOnlyHyphenThenViolationHappens() {
        underTest.setUserName("------");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenUsernameContainsAnUpperCaseLetterThenViolationHappens() {
        underTest.setUserName("somEname");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordHasEnoughLengthAndMeetsWithEveryRequirementThenEverythingGoesFine() {
        underTest.setPassword("passw0rd");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordHasMeetsWithEveryRequirementAndHasUpperCaseCharacterInItThenEverythingGoesFine() {
        underTest.setPassword("Passw0rd");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordContainsOnlyUpperCaseCharactersAndNumbersThenThisShouldBeFine() {
        underTest.setPassword("PASSW0RD");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordHasNotEnoughLengthThenViolationHappens() {
        underTest.setPassword("Adm1n");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordHasNotEnoughDigitThenViolationHappens() {
        underTest.setPassword("password");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordHasOnlydDigitsThenViolationHappens() {
        underTest.setPassword("123456789");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(EXPECTED_VIOLATION_AMOUNT, countViolationsExceptSpecificOne(constraintViolations));
    }

    @Test
    public void testClusterRequestCreationWhenPasswordContainsSpecialCharacterBesidesTheRequiredOnesThenThisShouldBeFine() {
        underTest.setPassword("Passw0rd$");
        Set<ConstraintViolation<ClusterRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertFalse(constraintViolations.stream().anyMatch(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())));
    }

    private long countViolationsExceptSpecificOne(Set<ConstraintViolation<ClusterRequest>> constraintViolations) {
        return constraintViolations.stream().filter(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())).count();
    }

}