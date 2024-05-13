package com.sequenceiq.environment.service.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

public class DummySecretService extends SecretService {

    private Map<String, String> store = new HashMap<>();

    public DummySecretService() {
        super(null, null, null);
    }

    @Override
    public String put(String key, String value) throws Exception {
        store.put(key, value);
        return key;
    }

    @Override
    public String get(String secret) {
        return store.get(secret);
    }

    @Override
    public String getByResponse(SecretResponse secretResponse) {
        return super.getByResponse(secretResponse);
    }

    @Override
    public void deleteByVaultSecretJson(String secret) {
        store.remove(secret);
    }

    @Override
    public List<String> listEntriesWithoutAppPath(String secretPathPrefix) {
        return super.listEntriesWithoutAppPath(secretPathPrefix);
    }

    @Override
    public void deleteByPathPostfix(String pathPrefix) {
        super.deleteByPathPostfix(pathPrefix);
    }

    @Override
    public boolean isSecret(String secret) {
        return super.isSecret(secret);
    }

    @Override
    public SecretResponse convertToExternal(String secret) {
        return new SecretResponse("", secret, null);
    }
}
