package com.sequenceiq.cloudbreak.util;

import java.security.SecureRandom;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.prng.SP800SecureRandomBuilder;
import org.springframework.stereotype.Component;

/**
 * Generates a 41 length password from the given 82 characters.
 * For the generation it uses {@code SecureRandom} as an entropy source
 * and SHA-512 hash based deterministic random bit generator.
 * A brute force attack needs to do 82^41 = 2.92E78 try.
 * To find hash collisions based on birthday attack it needs to do 82^(41/2) = 1.71E39 try.
 * Comparing it to 128 bit security level this value is more than the value 2^128 = 3.40E38.
 */
@Component
public class SHA512PasswordGenerator {

    private static final char[] CHARACTERS = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '.', ',', '?', '!',
            '@', '&', '^', '_',
            '+', '-', '*', '=', '/', '\\',
            '<', '>', '(', ')', '[', ']'
    };

    private static final int DEFAULT_PASSWORD_LENGTH = 41;

    public String generate() {
        SecureRandom drbg = new SP800SecureRandomBuilder(new SecureRandom(), true)
                .setPersonalizationString(Long.toString(System.nanoTime()).getBytes())
                .buildHash(new SHA512Digest(), null, true);
        StringBuilder password = new StringBuilder(DEFAULT_PASSWORD_LENGTH);
        for (int i = 0; i < DEFAULT_PASSWORD_LENGTH; i++) {
            password.append(CHARACTERS[drbg.nextInt(CHARACTERS.length)]);
        }
        return password.toString();
    }
}
