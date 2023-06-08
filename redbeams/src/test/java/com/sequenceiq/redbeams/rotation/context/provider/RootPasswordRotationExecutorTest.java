package com.sequenceiq.redbeams.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretRotationStep;
import com.sequenceiq.redbeams.rotation.RootPasswordRotationExecutor;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RootPasswordRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String ROOT_PASSWORD = "rootPassword";

    private static final String ROOT_PASSWORD_BACKUP = "rootPasswordBackup";

    private static final String CONNECTION_PASSWORD = "connectionPassword";

    private static final String CONNECTION_PASSWORD_BACKUP = "connectionPasswordBackup";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private DBStackToDatabaseStackConverter dbStackToDatabaseStackConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private SecretService secretService;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @InjectMocks
    private RootPasswordRotationExecutor underTest;

    @Test
    void rotateShouldSucceed() {
        DBStack dbStack = new DBStack();
        dbStack.setEnvironmentId(ENVIRONMENT_ID);
        dbStack.setOwnerCrn(Crn.fromString("crn:cdp:iam:us-west-1:default:user:owner@cloudera.com"));
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        dbStack.setDatabaseServer(databaseServer);
        when(databaseServer.getRootPasswordSecret()).thenReturn(ROOT_PASSWORD);
        when(dbStackService.getByCrn(RESOURCE_CRN)).thenReturn(dbStack);
        DatabaseServerConfig databaseServerConfig = mock(DatabaseServerConfig.class);
        when(databaseServerConfig.getConnectionPasswordSecret()).thenReturn(CONNECTION_PASSWORD);
        when(databaseServerConfigService.getByCrn(RESOURCE_CRN)).thenReturn(databaseServerConfig);
        when(secretService.getRotation(eq(ROOT_PASSWORD))).thenReturn(new RotationSecret(ROOT_PASSWORD, ROOT_PASSWORD_BACKUP));
        when(secretService.getRotation(eq(CONNECTION_PASSWORD))).thenReturn(new RotationSecret(CONNECTION_PASSWORD, CONNECTION_PASSWORD_BACKUP));
        when(credentialService.getCredentialByEnvCrn(any())).thenReturn(new Credential("credCrn", "name", "attributes", "accountId"));
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(dbStackToDatabaseStackConverter.convert(eq(dbStack))).thenReturn(new DatabaseStack(null, null, new HashMap<>(), null));
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        underTest.rotate(new RotationContext(RESOURCE_CRN));

        verify(dbStackService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(databaseServerConfigService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(secretService, times(1)).getRotation(eq(ROOT_PASSWORD));
        verify(secretService, times(1)).getRotation(eq(CONNECTION_PASSWORD));
        verify(credentialService, times(1)).getCredentialByEnvCrn(any());
        verify(credentialToCloudCredentialConverter, times(1)).convert(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(dbStackToDatabaseStackConverter, times(1)).convert(eq(dbStack));
        verify(resourceConnector, times(1)).updateDatabaseRootPassword(any(), any(), eq(ROOT_PASSWORD));
    }

    @Test
    void rotateShouldFailWhenSecretsAreNotInRotationState() {
        DBStack dbStack = new DBStack();
        dbStack.setEnvironmentId(ENVIRONMENT_ID);
        dbStack.setOwnerCrn(Crn.fromString("crn:cdp:iam:us-west-1:default:user:owner@cloudera.com"));
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        dbStack.setDatabaseServer(databaseServer);
        when(databaseServer.getRootPasswordSecret()).thenReturn(ROOT_PASSWORD);
        when(dbStackService.getByCrn(RESOURCE_CRN)).thenReturn(dbStack);
        DatabaseServerConfig databaseServerConfig = mock(DatabaseServerConfig.class);
        when(databaseServerConfig.getConnectionPasswordSecret()).thenReturn(CONNECTION_PASSWORD);
        when(databaseServerConfigService.getByCrn(RESOURCE_CRN)).thenReturn(databaseServerConfig);
        when(secretService.getRotation(eq(ROOT_PASSWORD))).thenReturn(new RotationSecret(ROOT_PASSWORD, null));
        when(secretService.getRotation(eq(CONNECTION_PASSWORD))).thenReturn(new RotationSecret(CONNECTION_PASSWORD, null));

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new RotationContext(RESOURCE_CRN)));

        assertEquals("Root password is not in rotation state in Vault, thus rotation is not possible.", secretRotationException.getMessage());
        assertEquals(RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD, secretRotationException.getFailedRotationStep());
        verify(dbStackService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(databaseServerConfigService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(secretService, times(1)).getRotation(eq(ROOT_PASSWORD));
        verify(secretService, times(1)).getRotation(eq(CONNECTION_PASSWORD));
        verify(credentialService, never()).getCredentialByEnvCrn(any());
        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(cloudPlatformConnectors, never()).get(any());
        verify(dbStackToDatabaseStackConverter, never()).convert(eq(dbStack));
    }

    @Test
    void rollbackShouldSucceed() {
        DBStack dbStack = new DBStack();
        dbStack.setEnvironmentId(ENVIRONMENT_ID);
        dbStack.setOwnerCrn(Crn.fromString("crn:cdp:iam:us-west-1:default:user:owner@cloudera.com"));
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        dbStack.setDatabaseServer(databaseServer);
        when(databaseServer.getRootPasswordSecret()).thenReturn(ROOT_PASSWORD);
        when(dbStackService.getByCrn(RESOURCE_CRN)).thenReturn(dbStack);
        DatabaseServerConfig databaseServerConfig = mock(DatabaseServerConfig.class);
        when(databaseServerConfig.getConnectionPasswordSecret()).thenReturn(CONNECTION_PASSWORD);
        when(databaseServerConfigService.getByCrn(RESOURCE_CRN)).thenReturn(databaseServerConfig);
        when(secretService.getRotation(eq(ROOT_PASSWORD))).thenReturn(new RotationSecret(ROOT_PASSWORD, ROOT_PASSWORD_BACKUP));
        when(secretService.getRotation(eq(CONNECTION_PASSWORD))).thenReturn(new RotationSecret(CONNECTION_PASSWORD, CONNECTION_PASSWORD_BACKUP));
        when(credentialService.getCredentialByEnvCrn(any())).thenReturn(new Credential("credCrn", "name", "attributes", "accountId"));
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(dbStackToDatabaseStackConverter.convert(eq(dbStack))).thenReturn(new DatabaseStack(null, null, new HashMap<>(), null));
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        underTest.rollback(new RotationContext(RESOURCE_CRN));

        verify(dbStackService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(databaseServerConfigService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(secretService, times(1)).getRotation(eq(ROOT_PASSWORD));
        verify(secretService, times(1)).getRotation(eq(CONNECTION_PASSWORD));
        verify(credentialService, times(1)).getCredentialByEnvCrn(any());
        verify(credentialToCloudCredentialConverter, times(1)).convert(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(dbStackToDatabaseStackConverter, times(1)).convert(eq(dbStack));
        verify(resourceConnector, times(1)).updateDatabaseRootPassword(any(), any(), eq(ROOT_PASSWORD_BACKUP));
    }

    @Test
    void finalizeShouldDoNothing() {
        verify(dbStackService, never()).getByCrn(eq(RESOURCE_CRN));
        verify(databaseServerConfigService, never()).getByCrn(eq(RESOURCE_CRN));
        verify(secretService, never()).getRotation(eq(ROOT_PASSWORD));
        verify(secretService, never()).getRotation(eq(CONNECTION_PASSWORD));
        verify(credentialService, never()).getCredentialByEnvCrn(any());
        verify(credentialToCloudCredentialConverter, never()).convert(any());
        verify(cloudPlatformConnectors, never()).get(any());
        verify(dbStackToDatabaseStackConverter, never()).convert(any());
    }
}