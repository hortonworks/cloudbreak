package com.sequenceiq.cloudbreak.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang3.RandomStringUtils;

public class PasswordUtil {

    private static final int PWD_LENGTH = 128;
    private static final int RADIX = 32;

    private PasswordUtil() {
    }

    public static String generatePassword() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            String raw = RandomStringUtils.randomAscii(PWD_LENGTH);
            byte[] digest = messageDigest.digest(raw.getBytes());
            return new BigInteger(1, digest).toString(RADIX);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
