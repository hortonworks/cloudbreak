package com.sequenceiq.caas.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;

public class EncryptionUtil {

    private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.HS256;

    private EncryptionUtil() {
    }

    public static byte[] getHmacKey() {
        return MacProvider.generateKey(ALGORITHM).getEncoded();
    }

}