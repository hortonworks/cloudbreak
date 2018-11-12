package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.type.KerberosType;

@RunWith(Parameterized.class)
public class KerberosRequestTest {

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private final long expectedViolationAmount;

    private final String name;

    private KerberosRequest underTest;

    private LocalValidatorFactoryBean localValidatorFactory;

    public KerberosRequestTest(String name, long expectedViolationAmount) {
        this.name = name;
        this.expectedViolationAmount = expectedViolationAmount;
    }

    @Before
    public void setUp() {
        underTest = new KerberosRequest();
        underTest.setType(KerberosType.EXISTING_FREEIPA);
        underTest.setPrincipal("testprincipal");
        underTest.setPassword("testpass");
        underTest.setUrl("url");
        underTest.setAdminUrl("url");
        underTest.setRealm("testRealm");
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Parameters(name = "{index}: name: {0} expectedViolationAmount: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"1.1.1.1", 0},
                {"1.1.1.1,2.2.2.2", 0},
                {"1.1.1.1,2.2.2.2,3.3.3.3", 0},
                {"255.255.255.255", 0},
                {"255.255.255.255,255.255.255.255", 0},
                {"1.1.1.256", 1},
                {"1.1.256.1", 1},
                {"1.256.1.1", 1},
                {"256.1.1.1", 1},
                {"1..1.1", 1},
                {"1.1.1.1,", 1},
                {"a.a.a.a", 1},
                {"123456", 1},
                {"", 1},
                {"asf", 1},
                {"somevalue%12", 1},
                {"somevalue;12", 1},
                {"somevalue/12", 1},
                {"somevalue/", 1},
                {"/", 1},
                {null, 0}
        });
    }

    @Test
    public void testBlueprintName() {
        underTest.setNameServers(name);
        Set<ConstraintViolation<KerberosRequest>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(expectedViolationAmount, countViolationsExceptSpecificOne(constraintViolations));
    }

    private long countViolationsExceptSpecificOne(Set<ConstraintViolation<KerberosRequest>> constraintViolations) {
        return constraintViolations.stream().filter(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())).count();
    }

}
