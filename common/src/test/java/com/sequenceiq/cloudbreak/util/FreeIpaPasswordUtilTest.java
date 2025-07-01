package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

class FreeIpaPasswordUtilTest {

    private static final Set<String> PATTERNS = Set.of("(?=.*[a-z]).*$", "(?=.*[A-Z]).*$", "(?=.*[0-9]).*$", ".*\\W+.*");

    @Test
    void testGeneratePasswordShouldCreatePwdWithCorrectSize() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        assertEquals(32, actual.length());
    }

    @Test
    void testGeneratePassword() {
        String actual = FreeIpaPasswordUtil.generatePassword();
        long characterClasses = PATTERNS.stream().filter(actual::matches).count();

        assertEquals(4, characterClasses);
    }

    @Test
    void testDoNotRepeatCharactersWhenStringLenghtIsTooShort() {
        String result = FreeIpaPasswordUtil.doNotRepeatCharacters("aa");
        assertEquals("aa", result);
    }

    @Test
    void testDoNotRepeatCharactersWhenThreeSameCharacter() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> FreeIpaPasswordUtil.doNotRepeatCharacters("aaa"));
        assertEquals("Cannot generate password without repeating characters", exception.getMessage());
    }

    @Test
    void testDoNotRepeatCharactersWhenInputContainsInvalidSequence() {
        String result = FreeIpaPasswordUtil.doNotRepeatCharacters("aaa1");
        assertNotEquals("aaa1", result);
    }

    @Test
    void testDoNotRepeatCharactersWhenInputDoesNotContainInvalidSequence() {
        String result = FreeIpaPasswordUtil.doNotRepeatCharacters("abcd");
        assertEquals("abcd", result);
    }
}