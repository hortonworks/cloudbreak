package com.sequenceiq.cloudbreak.service.vault;

import java.util.HashMap;
import java.util.Map;

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

    public String addFieldToSecret(String path, String pathIdentifier, String key, String value) {
        String realPath = path + '/' + pathIdentifier;
        VaultResponse response = operations.read(realPath);
        Map<String, Object> existingData = new HashMap<>();
        if (response != null && response.getData() != null) {
            existingData = response.getData();
        }
        existingData.put(key, value);
        operations.write(realPath, existingData);
        return realPath;
    }

    public String resolveSingleValue(String path, String key) {
        VaultResponse response = template.read(path);
        if (response != null && response.getData() != null) {
            return String.valueOf(response.getData().get(key));
        }
        return "";
    }

    public void deleteSecret(String path) {
        operations.delete(path);
    }

}
