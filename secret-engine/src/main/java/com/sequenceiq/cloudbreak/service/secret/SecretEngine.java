package com.sequenceiq.cloudbreak.service.secret;

import java.util.List;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

public interface SecretEngine {
    String put(String key, String value);

    boolean exists(String secret);

    String get(String secret);

    void delete(String secret);

    boolean isSecret(String secret);

    SecretResponse convertToExternal(String secret);

    String scarifySecret(String secret);

    List<String> listEntries(String secretPathPrefix);

    void cleanup(String path);
}
