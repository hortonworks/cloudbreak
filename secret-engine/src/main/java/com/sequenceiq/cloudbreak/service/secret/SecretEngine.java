package com.sequenceiq.cloudbreak.service.secret;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

public interface SecretEngine {

    String appPath();

    String enginePath();

    boolean isSecret(String vaultSecretJson);

    String put(@NotNull String secretPath, @NotNull Integer currentVersion, @NotNull Map<String, String> value);

    Map<String, String> getWithCache(@NotNull String secretPath, @NotNull Integer version);

    Map<String, String> getWithoutCache(@NotNull String secretPath);

    Optional<Integer> getVersion(String secretPath);

    void delete(String path, Integer version);

    List<String> listEntries(String secretPathPrefix);

}
