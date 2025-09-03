package com.sequenceiq.redbeams.rotation.context.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretRotationStep;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;
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
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withNewSecretMap(Map.of(
                        dbStack, Map.of(SecretMarker.DBSTACK_ROOT_PWD, newPassword),
                        databaseServerConfig, Map.of(SecretMarker.DBSERVER_CONFIG_ROOT_PWD, newPassword)
                ))
                .build();
        contexts.put(CommonSecretRotationStep.VAULT, vaultRotationContext);
        contexts.put(RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD, new RotationContext(resourceCrn));
        return contexts;
    }

    @Override
    public SecretType getSecret() {
        return RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
    }
}
