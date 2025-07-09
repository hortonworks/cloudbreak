package com.sequenceiq.cloudbreak.common.base64;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Util {

    private Base64Util() {

    }

    public static String encode(String nonBase64String) {
        return encode(nonBase64String.getBytes());
    }

    public static String encode(byte[] nonBase64Bytes) {
        return Base64.getEncoder().encodeToString(nonBase64Bytes);
    }

    public static String encodeToUrlString(byte[] nonBase64Bytes) {
        return Base64.getUrlEncoder().encodeToString(nonBase64Bytes);
    }

    public static byte[] decodeAsByteArray(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static byte[] decodeAsByteArray(byte[] base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static String decode(String base64String) {
        return new String(decodeAsByteArray(base64String), StandardCharsets.UTF_8);
    }
}
