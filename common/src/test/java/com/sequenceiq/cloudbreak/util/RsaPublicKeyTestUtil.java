package com.sequenceiq.cloudbreak.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class RsaPublicKeyTestUtil {

    private RsaPublicKeyTestUtil() {
    }

    /**
     * Builds an OpenSSH formatted RSA public key with a modulus of exactly {@code modulusBits} bits. The modulus is not a real RSA modulus, it is only
     * meant for tests that check the key length, and it is generated instantly unlike a prime.
     */
    public static String openSshRsaKey(int modulusBits) {
        BigInteger modulus = BigInteger.ONE.shiftLeft(modulusBits - 1).setBit(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (byte[] part : new byte[][]{"ssh-rsa".getBytes(StandardCharsets.US_ASCII), BigInteger.valueOf(65537).toByteArray(), modulus.toByteArray()}) {
                out.write(ByteBuffer.allocate(4).putInt(part.length).array());
                out.write(part);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "ssh-rsa " + Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
