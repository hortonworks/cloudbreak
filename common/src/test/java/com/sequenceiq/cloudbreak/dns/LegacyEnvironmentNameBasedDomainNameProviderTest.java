package com.sequenceiq.cloudbreak.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LegacyEnvironmentNameBasedDomainNameProviderTest {

    private LegacyEnvironmentNameBasedDomainNameProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegacyEnvironmentNameBasedDomainNameProvider();
    }

    @Test
    void testGetDomainWhenEnvironmentNameIsNull() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomainName(null, "anAccountName"));
        assertEquals(LegacyEnvironmentNameBasedDomainNameProvider.ENV_NAME_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenAccountNameIsNull() {
        String anEnvName = "anEnvName";
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> {
            underTest.getDomainName(anEnvName, null);
        });
        assertEquals(String.format(LegacyEnvironmentNameBasedDomainNameProvider.ACCOUNT_NAME_IS_EMTPY_FORMAT, anEnvName), iSE.getMessage());
    }

    @Test
    void testGetDomainWhenEnvironmentNameIsEmpty() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomainName("", "anAccountName"));
        assertEquals(LegacyEnvironmentNameBasedDomainNameProvider.ENV_NAME_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenAccountNameIsEmpty() {
        String anEnvName = "anEnvName";
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> {
            underTest.getDomainName(anEnvName, "");
        });
        assertEquals(String.format(LegacyEnvironmentNameBasedDomainNameProvider.ACCOUNT_NAME_IS_EMTPY_FORMAT, anEnvName), iSE.getMessage());
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

}