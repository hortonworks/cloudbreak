package com.sequenceiq.redbeams.rotation.context.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretRotationStep;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class ExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ROOT_PASSWORD_SECRET = "rootPasswordSecret";

    private static final String CONNECTION_PASSWORD_SECRET = "connectionPasswordSecret";

    private static final String NEW_PASSWORD = "newPassword";

    @Mock
    private PasswordGeneratorService passwordGeneratorService;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @InjectMocks
    private ExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform(CloudPlatform.AWS.name());
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(databaseServer.getRootPasswordSecret()).thenReturn(ROOT_PASSWORD_SECRET);
        dbStack.setDatabaseServer(databaseServer);
        when(dbStackService.getByCrn(eq(RESOURCE_CRN))).thenReturn(dbStack);
        DatabaseServerConfig databaseServerConfig = mock(DatabaseServerConfig.class);
        when(databaseServerConfig.getConnectionPasswordSecret()).thenReturn(CONNECTION_PASSWORD_SECRET);
        when(databaseServerConfigService.getByCrn(eq(RESOURCE_CRN))).thenReturn(databaseServerConfig);
        when(passwordGeneratorService.generatePassword(eq(Optional.of(CloudPlatform.AWS)))).thenReturn(NEW_PASSWORD);
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        assertThat(contexts).hasSize(2);
        assertThat(contexts).containsOnlyKeys(CommonSecretRotationStep.VAULT, RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD);

        assertThat(contexts.get(CommonSecretRotationStep.VAULT)).isInstanceOf(VaultRotationContext.class);
        VaultRotationContext vaultRotationContext = (VaultRotationContext) contexts.get(CommonSecretRotationStep.VAULT);
        assertEquals(RESOURCE_CRN, vaultRotationContext.getResourceCrn());
        Map<String, String> vaultPathSecretMap = vaultRotationContext.getNewSecretMap();
        assertThat(vaultPathSecretMap).containsOnlyKeys(ROOT_PASSWORD_SECRET, CONNECTION_PASSWORD_SECRET);
        assertEquals(NEW_PASSWORD, vaultPathSecretMap.get(ROOT_PASSWORD_SECRET));
        assertEquals(NEW_PASSWORD, vaultPathSecretMap.get(CONNECTION_PASSWORD_SECRET));

        assertThat(contexts.get(RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD)).isInstanceOf(RotationContext.class);
        RotationContext providerDatabaseRootPasswordRotationContext = contexts.get(RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD);
        assertEquals(RESOURCE_CRN, providerDatabaseRootPasswordRotationContext.getResourceCrn());
    }

}