package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class FreeIpaPasswordUtilTest {

    private static final Set<String> PATTERNS = Set.of("(?=.*[a-z]).*$", "(?=.*[A-Z]).*$", "(?=.*[0-9]).*$", ".*\\W+.*");

    @Test
    public void testGeneratePasswordShouldCreatePwdWithCorrectSize() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        assertEquals(32, actual.length());
    }

    @Test
    public void testGeneratePassword() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        long characterClasses = PATTERNS.stream().filter(actual::matches).count();

        assertEquals(4, characterClasses);
    }

}