package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

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
    public String appPath() {
        return appPath;
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public String put(String path, String value) {
        return put(path, Collections.singletonMap(VaultConstants.FIELD_SECRET, value));
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public String put(String path, Map<String, String> value) {
        LOGGER.info("Storing secret to {}", path);
        VaultSecret secret = convertToVaultSecret(enginePath, appPath + path);
        template.write(secret.getPath(), value);
        return gson().toJson(secret);
    }

    @Override
    public boolean exists(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            return response != null && response.getData() != null;
        }).orElse(false);
    }

    @Override
    @Cacheable(cacheNames = VaultConstants.CACHE_NAME)
    public String get(@NotNull String secret, @NotNull String field) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            return response != null && response.getData() != null && response.getData().containsKey(field) ?
                    String.valueOf(response.getData().get(field)) : null;
        }).orElse(null);
    }

    @Override
    @Cacheable(cacheNames = VaultConstants.CACHE_NAME)
    public RotationSecret getRotation(@NotNull String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            logRotationMeta(response);
            return response != null && response.getData() != null ?
                    new RotationSecret(String.valueOf(response.getData().get(VaultConstants.FIELD_SECRET)),
                            String.valueOf(response.getData().get(VaultConstants.FIELD_BACKUP))) : null;
        }).orElse(null);
    }

    private void logRotationMeta(VaultResponse response) {
        boolean ongoingRotation = response.getData().get(VaultConstants.FIELD_BACKUP) != null;
        LOGGER.info("Backup value is set: {}. Rotation secret metadata: {}", ongoingRotation, response.getMetadata());
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
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
