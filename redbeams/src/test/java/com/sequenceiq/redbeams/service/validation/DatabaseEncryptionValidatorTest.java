package com.sequenceiq.redbeams.service.validation;

import static com.sequenceiq.cloudbreak.common.database.TargetMajorVersion.VERSION14;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.common.model.AzureDatabaseType.AZURE_DATABASE_TYPE_KEY;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class DatabaseEncryptionValidatorTest {

    private static final String ENVIRONMENT_CRN = "env-crn";

    private static final String KEY_VAULT_URL = "keyVaultUrl";

    @InjectMocks
    private DatabaseEncryptionValidator underTest;

    @Mock
    private EnvironmentService environmentService;

    @Test
    void testValidateEncryptionShouldNotThrowExceptionWhenTheCloudPlatformIsNotAzure() {
        underTest.validateEncryption(AWS.name(), createDatabaseStack(FLEXIBLE_SERVER, "url"));
        verifyNoInteractions(environmentService);
    }

    @Test
    void testValidateEncryptionShouldNotThrowExceptionWhenEnvryptedSingleServerAndNoManagedIdentity() {
        underTest.validateEncryption(AZURE.name(), createDatabaseStack(SINGLE_SERVER, "url"));
        verifyNoInteractions(environmentService);
    }

    @Test
    void testValidateEncryptionShouldThrowExceptionWhenEnvryptedFlexibleServerAndNoManagedIdentity() {
        assertThrows(BadRequestException.class,
                () -> underTest.validateEncryption(AZURE.name(), createDatabaseStack(FLEXIBLE_SERVER, "url")));
    }

    @Test
    void testValidateEncryptionShouldNotThrowExceptionWhenEnvryptedFlexibleServerAndManagedIdentity() {
        assertThrows(BadRequestException.class,
                () -> underTest.validateEncryption(AZURE.name(), createDatabaseStack(FLEXIBLE_SERVER, "url")));
    }

    @Test
    void testValidateEncryptionDuringUpgradeShouldNotThrowExceptionWhenTheDatabaseIsEncryptedAndTheEnvironmentContainsEncryptionKey() {
        underTest.validateEncryptionDuringUpgrade(AZURE.name(), ENVIRONMENT_CRN, createDatabaseStack(FLEXIBLE_SERVER, "url"), VERSION14);
    }

    @Test
    void testValidateEncryptionDuringUpgradeShouldNotThrowExceptionWhenTheCloudPlatformIsNotAzure() {
        underTest.validateEncryptionDuringUpgrade(AWS.name(), ENVIRONMENT_CRN, createDatabaseStack(FLEXIBLE_SERVER, "url"), VERSION14);
        verifyNoInteractions(environmentService);
    }

    @Test
    void testValidateEncryptionDuringUpgradeShouldNotThrowExceptionWhenTheDatabaseIsNotEncrypted() {
        underTest.validateEncryptionDuringUpgrade(AZURE.name(), ENVIRONMENT_CRN, createDatabaseStack(FLEXIBLE_SERVER, null), VERSION14);
        verifyNoInteractions(environmentService);
    }

    @Test
    void testValidateEncryptionDuringUpgradeShouldThrowExceptionWhenTheDatabaseIsEncryptedAndTheEnvironmentNotContainsManagedIdentity() {
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironmentResponse(null));
        assertThrows(BadRequestException.class,
                () -> underTest.validateEncryptionDuringUpgrade(AZURE.name(), ENVIRONMENT_CRN, createDatabaseStack(SINGLE_SERVER, "url"), VERSION14));
    }

    private DatabaseServer createDatabaseStack(AzureDatabaseType azureDatabaseType, String keyVaultUrl) {
        return createDatabaseStack(azureDatabaseType, keyVaultUrl, null);
    }

    private DatabaseServer createDatabaseStack(AzureDatabaseType azureDatabaseType, String keyVaultUrl, String managedIdentity) {
        Map<String, Object> databaseServerParams = new HashMap<>();
        databaseServerParams.put(AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
        Optional.ofNullable(keyVaultUrl).ifPresent(url -> databaseServerParams.put(KEY_VAULT_URL, url));
        Optional.ofNullable(managedIdentity).ifPresent(identity ->
                databaseServerParams.put(PlatformParametersConsts.ENCRYPTION_USER_MANAGED_IDENTITY, identity));
        return DatabaseServer.builder()
                .withParams(databaseServerParams)
                .build();
    }

    private DetailedEnvironmentResponse createEnvironmentResponse(String userManagedIdentity) {
        return DetailedEnvironmentResponse.builder()
                .withAzure(AzureEnvironmentParameters.builder()
                        .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                                .withUserManagedIdentity(userManagedIdentity)
                                .build())
                        .build())
                .build();
    }
}