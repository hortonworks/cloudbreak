package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

@Component("VaultKvV1Engine")
@ConditionalOnBean(VaultConfig.class)
public class VaultKvV1Engine extends AbstractVaultEngine<VaultKvV1Engine> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultKvV1Engine.class);

    @Value("${vault.kv.engine.path:}")
    private String enginePath;

    @Value("#{'${vault.kv.engine.path:}/${secret.application:}/'}")
    private String appPath;

    private VaultTemplate template;

    @Inject
    public VaultKvV1Engine(VaultTemplate template) {
        this.template = template;
    }

    @Override
    public String put(String path, String value) {
        LOGGER.info("Storing secret to {}", path);
        VaultSecret secret = convertToVaultSecret(enginePath, appPath + path);
        template.write(secret.getPath(), Collections.singletonMap("secret", value));
        return gson().toJson(secret);
    }

    @Override
    public boolean isExists(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            return response != null && response.getData() != null;
        }).orElse(false);
    }

    @Override
    @Cacheable(cacheNames = "vaultCache")
    public String get(@NotNull String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            return response != null && response.getData() != null ? String.valueOf(response.getData().get("secret")) : null;
        }).orElse(null);
    }

    @Override
    @CacheEvict(cacheNames = "vaultCache", allEntries = true)
    public void delete(String secret) {
        Optional.ofNullable(convertToVaultSecret(secret)).ifPresent(s -> template.delete(s.getPath()));
    }

    @Override
    public SecretResponse convertToExternal(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret))
                .map(s -> new SecretResponse(null, s.getPath()))
                .orElse(null);
    }

    @Override
    protected Class<VaultKvV1Engine> clazz() {
        return VaultKvV1Engine.class;
    }

    public List<String> listEntries(String path) {
        return Collections.EMPTY_LIST;
    }

    public void cleanup(String path) {
        // Do Nothing.
    }
}
