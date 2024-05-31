package com.sequenceiq.cloudbreak.service.secret;

import java.util.List;
import java.util.Map;

public interface SecretEngine {

    String appPath();

    String enginePath();

    boolean isSecret(String vaultSecretJson);

    String put(String secretPath, Map<String, String> value);

    Map<String, String> getWithCache(String secretPath);

    Map<String, String> getWithoutCache(String secretPath);

    void delete(String path);

    List<String> listEntries(String secretPathPrefix);

}
