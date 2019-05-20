package com.sequenceiq.cloudbreak.service.secret;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

public interface SecretEngine {
    String put(String key, String value);

    boolean isExists(String secret);

    String get(String secret);

    void delete(String secret);

    boolean isSecret(String secret);

    SecretResponse convertToExternal(String secret);

    String scarifySecret(String secret);
}
