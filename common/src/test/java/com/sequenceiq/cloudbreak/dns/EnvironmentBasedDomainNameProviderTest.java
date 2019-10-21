package com.sequenceiq.cloudbreak.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EnvironmentBasedDomainNameProviderTest {

    private EnvironmentBasedDomainNameProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnvironmentBasedDomainNameProvider();
    }

    @Test
    void testGetDomainWhenEnvironmentNameIsNull() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomainName(null, "anAccountName"));
        assertEquals(EnvironmentBasedDomainNameProvider.ENV_NAME_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenAccountNameIsNull() {
        String anEnvName = "anEnvName";
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> {
            underTest.getDomainName(anEnvName, null);
        });
        assertEquals(String.format(EnvironmentBasedDomainNameProvider.ACCOUNT_NAME_IS_EMTPY_FORMAT, anEnvName), iSE.getMessage());
    }

    @Test
    void testGetDomainWhenEnvironmentNameIsEmpty() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomainName("", "anAccountName"));
        assertEquals(EnvironmentBasedDomainNameProvider.ENV_NAME_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenAccountNameIsEmpty() {
        String anEnvName = "anEnvName";
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> {
            underTest.getDomainName(anEnvName, "");
        });
        assertEquals(String.format(EnvironmentBasedDomainNameProvider.ACCOUNT_NAME_IS_EMTPY_FORMAT, anEnvName), iSE.getMessage());
    }

    @Test
    void testGetDomain() {
        String envName = "an-env-name";
        String accountName = "an-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", "cloudera.site");

        String result = underTest.getDomainName(envName, accountName);

        String expected = String.format("%s.%s.cloudera.site", "an-env-n", accountName);
        assertEquals(expected, result);
    }

    @Test
    void testGetDomainwhenDashIn8thChar() {
        String envName = "an-envi-name";
        String accountName = "an-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", "cloudera.site");

        String result = underTest.getDomainName(envName, accountName);

        String expected = String.format("%s.%s.cloudera.site", "an-envi", accountName);
        assertEquals(expected, result);
    }

    @Test
    void testGetDomainWhenRootDomainIsOverriddenAndStartsWithDot() {
        String envName = "an-env-name";
        String accountName = "an-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        String result = underTest.getDomainName(envName, accountName);

        String expected = String.format("%s.%s.mytest.local", "an-env-n", accountName);
        assertEquals(expected, result);
    }

    @Test
    void testGetDomainWhenEnvironmentNameContainsIllegalChars() {
        String envName = "anEnvName$%%$";
        String accountName = "anaccountname";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getDomainName(envName, accountName));

        String actualMessage = illegalStateException.getMessage();
        assertTrue(actualMessage.startsWith("The generated domain(")
                && actualMessage.contains("doesn't match with domain allowed pattern"));
    }

    @Test
    void testGetDomainWhenAccountNameContainsIllegalChars() {
        String envName = "an-env-name";
        String accountName = "anAccount$%$$$Name";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getDomainName(envName, accountName));

        String actualMessage = illegalStateException.getMessage();
        assertTrue(actualMessage.startsWith("The generated domain(")
                && actualMessage.contains("doesn't match with domain allowed pattern"));
    }

    @Test
    void testGetDomainWhenTheGeneratedDomainLongerThen62Chars() {
        String envName = "an-loooooooooong-env-name";
        String accountName = "an-loooooooooooooong-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", "some-subdomain.mytest.local");

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getDomainName(envName, accountName));

        String actualMessage = illegalStateException.getMessage();
        assertTrue(actualMessage.startsWith("The length of the generated domain(")
                && actualMessage.contains("is longer than the allowed 62 characters"));
    }

    @Test
    void testGetCommonNameWhenTheEndpointNameIsLessThan17AndEnvNameLessThan8Chars() {
        String endpointName = "test-cl-master0";
        String envName = "shrt-nv";
        String accountName = "xcu2-8y8x";
        String rootDomain = "workload-local.cloudera.site";
        ReflectionTestUtils.setField(underTest, "rootDomain", rootDomain);

        String commonName = underTest.getCommonName(endpointName, envName, accountName);

        String expected = String.format("bbd9025ff9fd7c4d.%s.%s.%s", envName, accountName, rootDomain);
        assertEquals(expected, commonName);
    }

    @Test
    void testGetCommonNameWhenTheEndpointNameIsLongerThan17AndEnvNameLongerThan8Chars() {
        String endpointName = "test-cl-longyloooooooooooong-name-master0";
        String envName = "notashort-env-name-as28chars";
        String accountName = "xcu2-8y8x";
        String rootDomain = "workload-local.cloudera.site";
        ReflectionTestUtils.setField(underTest, "rootDomain", rootDomain);

        String commonName = underTest.getCommonName(endpointName, envName, accountName);

        String expected = String.format("a7c2a45fc8f917fe.%s.%s.%s", envName.substring(0, 8), accountName, rootDomain);
        assertEquals(expected, commonName);
    }
}