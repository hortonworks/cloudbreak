package com.sequenceiq.cloudbreak.service.secret;

public interface SecretEngine {
    String put(String key, String value);

    boolean isExists(String secret);

    String get(String secret);

    void delete(String secret);

    boolean isSecret(String secret);
}
