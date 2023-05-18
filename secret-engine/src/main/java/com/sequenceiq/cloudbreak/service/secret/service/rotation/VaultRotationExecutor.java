package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretGenerator;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Component
public class VaultRotationExecutor implements RotationExecutor<VaultRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRotationExecutor.class);

    @Inject
    private SecretService secretService;

    @Inject
    private Optional<List<SecretGenerator>> secretGenerators;

    private final Map<Class<? extends SecretGenerator>, SecretGenerator> secretGeneratorMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void setUp() {
        secretGenerators.ifPresent(generators -> generators.forEach(secretGenerator ->
                secretGeneratorMap.put(secretGenerator.getClass(), secretGenerator)));
    }

    @Override
    public void rotate(VaultRotationContext rotationContext) {
        rotationContext.getSecretGenerators().forEach((vaultSecret, secretGeneratorClass) -> {
            try {
                if (!secretService.getRotation(vaultSecret).isRotation()) {
                    SecretGenerator secretGenerator = secretGeneratorMap.get(secretGeneratorClass);
                    Map<String, Object> secretGeneratorArguments = Map.of();
                    if (rotationContext.getSecretGeneratorArguments().containsKey(secretGeneratorClass)) {
                        secretGeneratorArguments = rotationContext.getSecretGeneratorArguments().get(secretGeneratorClass);
                    }
                    secretService.putRotation(vaultSecret, secretGenerator.generate(secretGeneratorArguments));
                }
            } catch (Exception e) {
                LOGGER.error("Error during {} secret rotation.", vaultSecret, e);
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void rollback(VaultRotationContext rotationContext) {
        rotationContext.getSecretGenerators().forEach((vaultSecret, generator) -> {
            try {
                RotationSecret rotationSecret = secretService.getRotation(vaultSecret);
                if (rotationSecret.isRotation()) {
                    secretService.update(vaultSecret, rotationSecret.getBackupSecret());
                }
            } catch (Exception e) {
                LOGGER.error("Error during {} secret rollback.", vaultSecret, e);
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void finalize(VaultRotationContext rotationContext) {
        rotationContext.getSecretGenerators().forEach((vaultSecret, generator) -> {
            try {
                RotationSecret rotationSecret = secretService.getRotation(vaultSecret);
                if (rotationSecret.isRotation()) {
                    secretService.update(vaultSecret, rotationSecret.getSecret());
                }
            } catch (Exception e) {
                LOGGER.error("Error during {} secret finalization.", vaultSecret, e);
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public SecretRotationStep getType() {
        return SecretRotationStep.VAULT;
    }
}
