package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class FreeIpaPasswordUtilTest {

    private static final Set<String> PATTERNS = Set.of("(?=.*[a-z]).*$", "(?=.*[A-Z]).*$", "(?=.*[0-9]).*$", ".*\\W+.*");

    @Test
    void testGeneratePasswordShouldCreatePwdWithCorrectSize() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        assertEquals(32, actual.length());
    }

    @RepeatedTest(10000)
    void testGeneratePassword() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        long characterClasses = PATTERNS.stream().filter(actual::matches).count();

        assertEquals(4, characterClasses);
        assertFalse(FreeIpaPasswordUtil.hasTripleRepeatingCharacters(actual));
    }

    @Test
    void testHasTripleRepeatingCharactersWhenNull() {
        assertFalse(FreeIpaPasswordUtil.hasTripleRepeatingCharacters(null));
    }

    @Test
    void testHasTripleRepeatingCharactersWhenLenghtIsShort() {
        assertFalse(FreeIpaPasswordUtil.hasTripleRepeatingCharacters("aa"));
    }

    @Test
    void testHasTripleRepeatWhenNoTripleRepeatingCharacters() {
        assertFalse(FreeIpaPasswordUtil.hasTripleRepeatingCharacters("aabbccdd"));
    }

    @Test
    void testHasTripleRepeatWhenTripeRepeatingCharacters() {
        assertTrue(FreeIpaPasswordUtil.hasTripleRepeatingCharacters("aaa"));
    }
}