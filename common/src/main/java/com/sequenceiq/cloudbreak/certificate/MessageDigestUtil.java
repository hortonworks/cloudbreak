package com.sequenceiq.cloudbreak.certificate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDigestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDigestUtil.class);

    private MessageDigestUtil() {

    }

    public static String signatureSHA512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(input.getBytes());
            return Hex.encodeHexString(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can not find SHA-512", e);
            throw new IllegalStateException("Can not find SHA-512", e);
        }
    }
}
