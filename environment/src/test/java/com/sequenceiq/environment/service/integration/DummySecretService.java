package com.sequenceiq.environment.service.integration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

public class DummySecretService extends SecretService {

    // Static and synchronized on purpose. The integration tests spin up several cached Spring contexts, each with its
    // own DummySecretService instance. Secrets are written via the injected SecretService (the test's own context) but
    // read back via SecretProxy.getRaw() -> StaticApplicationContext, which resolves whichever context initialized last.
    // A per-instance store would miss across contexts (-> null secret -> HTTP 500 'argument "content" is null'), so all
    // instances must share one map. synchronizedMap covers the [Test worker] write vs [http-nio] read visibility, and a
    // null-key-tolerant HashMap (not ConcurrentHashMap) mirrors the real SecretService returning null for get(null).
    private static final Map<String, String> STORE = Collections.synchronizedMap(new HashMap<>());

    public DummySecretService() {
        super(null, null, null);
    }

    @Override
    public String put(String key, String value) throws Exception {
        STORE.put(key, value);
        return key;
    }

    @Override
    public String get(String secret) {
        return STORE.get(secret);
    }

    @Override
    public String getByResponse(SecretResponse secretResponse) {
        return super.getByResponse(secretResponse);
    }

    @Override
    public void deleteByVaultSecretJson(String secret) {
        STORE.remove(secret);
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
