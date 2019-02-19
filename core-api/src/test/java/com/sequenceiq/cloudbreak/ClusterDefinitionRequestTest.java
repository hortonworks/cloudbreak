package com.sequenceiq.cloudbreak;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;

@RunWith(Parameterized.class)
public class ClusterDefinitionRequestTest {

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private final long expectedViolationAmount;

    private final String name;

    private ClusterDefinitionV4Request underTest;

    private LocalValidatorFactoryBean localValidatorFactory;

    public ClusterDefinitionRequestTest(String name, long expectedViolationAmount) {
        this.name = name;
        this.expectedViolationAmount = expectedViolationAmount;
    }

    @Before
    public void setUp() {
        underTest = new ClusterDefinitionV4Request();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Parameters(name = "{index}: name: {0} expectedViolationAmount: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Data Lake: Apache Ranger, Apache Atlas, Apache Hive Metastore", 0},
                {"Some-Passw0rd", 0},
                {"doin'n some cluster definition?!", 0},
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

    @Test
    public void testClusterDefinitionName() {
        underTest.setName(name);
        Set<ConstraintViolation<ClusterDefinitionV4Request>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(expectedViolationAmount, countViolationsExceptSpecificOne(constraintViolations));
    }

    private long countViolationsExceptSpecificOne(Set<ConstraintViolation<ClusterDefinitionV4Request>> constraintViolations) {
        return constraintViolations.stream().filter(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())).count();
    }

}
