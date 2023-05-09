package com.sequenceiq.cloudbreak.service.secret.domain;

public class RotationSecret {

    private String secret;

    private String backupSecret;

    public RotationSecret(String secret, String backupSecret) {
        this.secret = convertNullString(secret);
        this.backupSecret = convertNullString(backupSecret);
    }

    public String getSecret() {
        return secret;
    }

    public String getBackupSecret() {
        return backupSecret;
    }

    public boolean isRotation() {
        return backupSecret != null;
    }

    private String convertNullString(String input) {
        if ("null".equalsIgnoreCase(input)) {
            return null;
        }
        return input;
    }
}
