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

    public void writeSecrets(String path, String userName, String password) {
        Map<String, String> data = new HashMap<>();
        data.put("username", userName);
        data.put("password", password);
        operations.write(path, data);
    }

    public Map<String, Object> findOutnewFeature(String key) {
        VaultResponse response = template.read(key);
        return response.getData();
    }

}
