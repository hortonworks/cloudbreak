package com.sequenceiq.cloudbreak.util.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.security.SecureRandom;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.prng.SP800SecureRandomBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordGeneratorTest {

    @Spy
    private SecureRandom secureRandom = new SecureRandom();

    private PasswordGenerator underTest;

    @Test
    public void testRequiredEntropyIsNotTooBig() {
        underTest.generate();

        verify(secureRandom, times(1)).generateSeed(32);
        verifyNoMoreInteractions(secureRandom);
    }

    @BeforeEach
    public void setUp() {
        underTest = PasswordGenerator.builder()
                .prefix(CharSet.ALPHABETIC_LOWER_CASE, 3)
                .mandatoryCharacters(CharSet.SAFE_SPECIAL_CHARACTERS)
                .bodyPart(CharSet.ALPHABETIC_LOWER_CASE, 8)
                .bodyPart(CharSet.ALPHABETIC_UPPER_CASE, 8)
                .bodyPart(CharSet.NUMERIC, 8)
                .randomSupplier(() -> new SP800SecureRandomBuilder(secureRandom, false)
                        .setPersonalizationString(Long.toString(System.nanoTime()).getBytes())
                        .buildHash(new SHA512Digest(), null, false))
                .build();
    }

    @Test
    public void testPasswordLength() {
        String password = underTest.generate();

        assertEquals(32, password.length());
    }

    @Test
    public void testPasswordFormat() {
        String password = underTest.generate();

        assertFirstNCharacterInSet(password, CharSet.ALPHABETIC_LOWER_CASE, 3);

        assertContainsCharactersFromSet(password, 3, 32, CharSet.NUMERIC, 8);
        assertContainsCharactersFromSet(password, 3, 32, CharSet.ALPHABETIC_LOWER_CASE, 8);
        assertContainsCharactersFromSet(password, 3, 32, CharSet.ALPHABETIC_UPPER_CASE, 8);

        assertAllCharactersPresent(password, 3, 32, CharSet.SAFE_SPECIAL_CHARACTERS);
    }

    private void assertFirstNCharacterInSet(String password, CharSet charSet, int n) {
        for (int i = 0; i < n; i++) {
            assertTrue(charSet.getValues().contains(password.charAt(i)),
                    "Password character at " + i + "th place must be one of the characters " + charSet.getValues() + " actual value is " + password.charAt(i));
        }
    }

    private void assertContainsCharactersFromSet(String password, int start, int end, CharSet charSet, int requiredCount) {
        int numberOfCharactersFromSet = getNumberOfCharactersFromSet(password, start, end, charSet);
        String message = String.format("Password %s in range %s-%s must contain %s characters from values %s. Actual value: %s.",
                password, start, end, requiredCount, charSet.getValues(), numberOfCharactersFromSet);
        assertEquals(requiredCount, numberOfCharactersFromSet, message);
    }

    private void assertAllCharactersPresent(String password, int start, int end, CharSet charSet) {
        String message = String.format("Password %s in range %s-%s must contain all characters %s.", password, start, end, charSet.getValues());
        assertTrue(allCharactersIsPresent(password, start, end, charSet), message);
    }

    private int getNumberOfCharactersFromSet(String password, int start, int end, CharSet charSet) {
        int result = 0;
        for (int i = start; i < end; i++) {
            if (charSet.getValues().contains(password.charAt(i))) {
                result++;
            }
        }
        return result;
    }

    private boolean allCharactersIsPresent(String password, int start, int end, CharSet charSet) {
        for (char character : charSet.getValues()) {
            if (!password.substring(start, end).contains(Character.toString(character))) {
                return false;
            }
        }
        return true;
    }
}