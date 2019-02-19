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

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;

@RunWith(Parameterized.class)
public class KerberosFreeIpaTest {

    private static final String TEST_ADMIN_URL = "http://someadminurl.com";

    private static final String TEST_URL = "http://someadminurl.com";

    private static final String TEST_REALM = "someRealm";

    private static final String TEST_NAMESERVER_ADDRESS = "1.1.1.1";

    private static final String NOT_NULL_VIOLATION_TEMPLATE = "{javax.validation.constraints.NotNull.message}";

    private final long expectedViolationAmount;

    private final String nameServers;

    private final String url;

    private final String adminUrl;

    private final String realm;

    private FreeIPAKerberosDescriptor underTest;

    private LocalValidatorFactoryBean localValidatorFactory;

    public KerberosFreeIpaTest(String url, String adminUrl, String realm, String nameServers, long expectedViolationAmount) {
        this.nameServers = nameServers;
        this.url = url;
        this.adminUrl = adminUrl;
        this.realm = realm;
        this.expectedViolationAmount = expectedViolationAmount;
    }

    @Before
    public void setUp() {
        underTest = new FreeIPAKerberosDescriptor();
        underTest.setPassword("testpass");
        underTest.setPrincipal("soeprincipal");
        underTest.setRealm(realm);
        underTest.setAdminUrl(adminUrl);
        underTest.setUrl(url);
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @Parameters(name = "{index}: nameServers: {0} expectedViolationAmount: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.1.1.1", 0},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.1.1.1,2.2.2.2", 0},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.1.1.1,2.2.2.2,3.3.3.3", 0},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "255.255.255.255", 0},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "255.255.255.255,255.255.255.255", 0},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.1.1.256", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.1.256.1", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.256.1.1", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "256.1.1.1", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1..1.1", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "1.1.1.1,", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "a.a.a.a", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "123456", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "asf", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "somevalue%12", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "somevalue;12", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "somevalue/12", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "somevalue/", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, "/", 1},
                {TEST_URL, TEST_ADMIN_URL, TEST_REALM, null, 0},
                {"", TEST_ADMIN_URL, TEST_REALM, TEST_NAMESERVER_ADDRESS, 1},
                {null, TEST_ADMIN_URL, TEST_REALM, TEST_NAMESERVER_ADDRESS, 1},
                {TEST_URL, "", TEST_REALM, TEST_NAMESERVER_ADDRESS, 1},
                {TEST_URL, null, TEST_REALM, TEST_NAMESERVER_ADDRESS, 1},
                {TEST_URL, TEST_ADMIN_URL, "", TEST_NAMESERVER_ADDRESS, 1},
                {TEST_URL, TEST_ADMIN_URL, null, TEST_NAMESERVER_ADDRESS, 1},
                {null, null, null, TEST_NAMESERVER_ADDRESS, 3},
                {null, null, null, "", 4},
                {null, null, null, null, 3}
        });
    }

    @Test
    public void testClusterDefinitionName() {
        underTest.setNameServers(nameServers);

        Set<ConstraintViolation<FreeIPAKerberosDescriptor>> constraintViolations = localValidatorFactory.validate(underTest);
        Assert.assertEquals(expectedViolationAmount, countViolationsExceptSpecificOne(constraintViolations));
    }

    private long countViolationsExceptSpecificOne(Set<ConstraintViolation<FreeIPAKerberosDescriptor>> constraintViolations) {
        return constraintViolations.stream().filter(violation -> !NOT_NULL_VIOLATION_TEMPLATE.equalsIgnoreCase(violation.getMessageTemplate())).count();
    }

}
