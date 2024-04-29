package com.sequenceiq.environment.service.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

public class DummySecretService extends SecretService {

    private Map<String, String> store;

    public DummySecretService() {
        super(null, null);
    }

    @Override
    public void init() {
        store = new HashMap<>();
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
    public void delete(String secret) {
        store.remove(secret);
    }

    @Override
    public List<String> listEntries(String secretPathPrefix) {
        return super.listEntries(secretPathPrefix);
    }

    @Override
    public void cleanup(String pathPrefix) {
        super.cleanup(pathPrefix);
    }

    @Override
    public boolean isSecret(String secret) {
        return super.isSecret(secret);
    }

    @Override
    public SecretResponse convertToExternal(String secret) {
        return new SecretResponse("", secret);
    }
}
