package com.sequenceiq.cloudbreak.service.vault;

import java.security.InvalidKeyException;
import java.util.Collections;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@Service
public class VaultService {

    @Inject
    private VaultOperations operations;

    @Inject
    private VaultTemplate template;

    public String addFieldToSecret(String path, String value) throws Exception {
        VaultResponse response = operations.read(path);
        if (response != null && response.getData() != null) {
            throw new InvalidKeyException(String.format("Path: %s already exists!", path));
        }
        operations.write(path, Collections.singletonMap("secret", value));
        return path;
    }

    public String resolveSingleValue(String path) {
        VaultResponse response = template.read(path);
        if (response != null && response.getData() != null) {
            return String.valueOf(response.getData().get("secret"));
        }
        return "";
    }

    public void deleteSecret(String path) {
        operations.delete(path);
    }

}
