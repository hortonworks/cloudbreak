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
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomain(null, "anAccountName"));
        assertEquals(EnvironmentBasedDomainNameProvider.NAMES_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenAccountNameIsNull() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomain("anEnvName", null));
        assertEquals(EnvironmentBasedDomainNameProvider.NAMES_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenEnvironmentNameIsEmpty() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomain("", "anAccountName"));
        assertEquals(EnvironmentBasedDomainNameProvider.NAMES_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomainWhenAccountNameIsEmpty() {
        IllegalStateException iSE = assertThrows(IllegalStateException.class, () -> underTest.getDomain("anEnvName", ""));
        assertEquals(EnvironmentBasedDomainNameProvider.NAMES_SHOULD_BE_SPECIFIED_MSG, iSE.getMessage());
    }

    @Test
    void testGetDomain() {
        String envName = "an-env-name";
        String accountName = "an-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", "cloudera.site");

        String result = underTest.getDomain(envName, accountName);

        String expected = String.format("%s.%s.cloudera.site", envName, accountName);
        assertEquals(expected, result);
    }

    @Test
    void testGetDomainWhenRootDomainIsOverriddenAndStartsWithDot() {
        String envName = "an-env-name";
        String accountName = "an-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        String result = underTest.getDomain(envName, accountName);

        String expected = String.format("%s.%s.mytest.local", envName, accountName);
        assertEquals(expected, result);
    }

    @Test
    void testGetDomainWhenEnvironmentNameContainsIllegalChars() {
        String envName = "anEnvName$%%$";
        String accountName = "anaccountname";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getDomain(envName, accountName));

        String actualMessage = illegalStateException.getMessage();
        assertTrue(actualMessage.startsWith("The generated domain(")
                && actualMessage.contains("doesn't match with domain allowed pattern"));
    }

    @Test
    void testGetDomainWhenAccountNameContainsIllegalChars() {
        String envName = "an-env-name";
        String accountName = "anAccount$%$$$Name";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getDomain(envName, accountName));

        String actualMessage = illegalStateException.getMessage();
        assertTrue(actualMessage.startsWith("The generated domain(")
                && actualMessage.contains("doesn't match with domain allowed pattern"));
    }

    @Test
    void testGetDomainWhenTheGeneratedDomainLongerThen62Chars() {
        String envName = "an-loooooooooong-env-name";
        String accountName = "an-loooooooooooooong-account-name";
        ReflectionTestUtils.setField(underTest, "rootDomain", ".mytest.local");

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getDomain(envName, accountName));

        String actualMessage = illegalStateException.getMessage();
        assertTrue(actualMessage.startsWith("The length of the generated domain(")
                && actualMessage.contains("is longer than the allowed 62 characters"));
    }
}