package com.sequenceiq.cloudbreak.util;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class FreeIpaPasswordUtilTest {

    private static final Set<String> PATTERNS = Set.of("(?=.*[a-z]).*$", "(?=.*[A-Z]).*$", "(?=.*[0-9]).*$", ".*\\W+.*");

    @Test
    public void testGeneratePasswordShouldCreatePwdWithCorrectSize() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        Assert.assertEquals(32, actual.length());
    }

    @Test
    public void testGeneratePassword() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        long characterClasses = PATTERNS.stream().filter(actual::matches).count();

        Assert.assertEquals(4, characterClasses);
    }

}