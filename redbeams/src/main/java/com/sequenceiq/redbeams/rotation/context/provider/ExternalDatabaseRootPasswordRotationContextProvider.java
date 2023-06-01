package com.sequenceiq.redbeams.rotation.context.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class ExternalDatabaseRootPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private PasswordGeneratorService passwordGeneratorService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        DBStack dbStack = dbStackService.getByCrn(resourceCrn);
        DatabaseServerConfig databaseServerConfig = databaseServerConfigService.getByCrn(resourceCrn);
        String newPassword = passwordGeneratorService.generatePassword(Optional.of(CloudPlatform.valueOf(dbStack.getCloudPlatform())));
        Map<String, String> newSecretMap = new HashMap<>();
        newSecretMap.put(dbStack.getDatabaseServer().getRootPasswordSecret(), newPassword);
        newSecretMap.put(databaseServerConfig.getConnectionPasswordSecret(), newPassword);
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withVaultPathSecretMap(newSecretMap)
                .build();
        contexts.put(SecretRotationStep.VAULT, vaultRotationContext);
        contexts.put(SecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD, new RotationContext(resourceCrn));
        return contexts;
    }

    @Override
    public SecretType getSecret() {
        return RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
    }
}
