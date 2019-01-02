package com.sequenceiq.cloudbreak.service.secret;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;

public interface SecretEngine {
    String put(String key, String value);

    boolean isExists(String secret);

    String get(String secret);

    void delete(String secret);

    boolean isSecret(String secret);

    SecretV4Response convertToExternal(String secret);

    String scarifySecret(String secret);
}
