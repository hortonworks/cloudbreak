package com.sequenceiq.cloudbreak.service.secret;

public interface SecretEngine {
    String put(String key, String value);

    boolean isExists(String key);

    String get(String key);

    void delete(String key);

    boolean isSecret(String value);
}
