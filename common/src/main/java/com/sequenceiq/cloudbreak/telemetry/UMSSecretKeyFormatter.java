package com.sequenceiq.cloudbreak.telemetry;

import java.util.stream.Collectors;

public class UMSSecretKeyFormatter {

    private UMSSecretKeyFormatter() {
    }

    public static String formatSecretKey(String keyType, String secretKey) {
        if ("ECDSA".equals(keyType)) {
            return secretKey.trim().lines().collect(Collectors.joining("\\n"));
        } else {
            return secretKey;
        }
    }
}
