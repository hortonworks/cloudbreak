package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_KEYVAULT_KEY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_MANAGED_IDENTITY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.msi.models.Identity;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationPoller;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureEncryptionResourcesValidationTest {
    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private DiskEncryptionSetCreationPoller diskEncryptionSetCreationPoller;

    @Mock
    private Retry retryService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AzurePermissionValidator azurePermissionValidator;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private AzureEncryptionResources underTest;

    @Test
    void tesstValidateEncryptionParametersMissingCloudResource() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        // WHEN
        BadRequestException actualException = assertThrows(BadRequestException.class,
                () -> underTest.validateEncryptionParameters(validationRequest));
        // THEN
        assertEquals("Vault key and vault resource group is mandatory parameters", actualException.getMessage());
    }

    @Test
    void tesstValidateEncryptionParametersRoleBased() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        cloudResourceMap.put(AZURE_MANAGED_IDENTITY, createCloudResource(AZURE_MANAGED_IDENTITY, "managedIdentity", "managedIdentityReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(azureClient.getVaultNameFromEncryptionKeyUrl("keyVaultReference")).thenReturn("vaultName");
        Identity identity = mock(Identity.class);
        when(azureClient.getIdentityById("managedIdentityReference")).thenReturn(identity);
        Vault vault = mock(Vault.class);
        when(vault.roleBasedAccessControlEnabled()).thenReturn(true);
        when(azureClient.getKeyVault("resourceGroup", "vaultName")).thenReturn(vault);
        // WHEN
        underTest.validateEncryptionParameters(validationRequest);
        // THEN
        verify(azurePermissionValidator).validateCMKManagedIdentityPermissions(azureClient, identity, vault);
    }

    @Test
    void tesstValidateEncryptionParametersAccesPolicies() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        cloudResourceMap.put(AZURE_MANAGED_IDENTITY, createCloudResource(AZURE_MANAGED_IDENTITY, "managedIdentity", "managedIdentityReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(azureClient.getVaultNameFromEncryptionKeyUrl("keyVaultReference")).thenReturn("vaultName");
        Identity identity = mock(Identity.class);
        when(identity.principalId()).thenReturn("id");
        when(azureClient.getIdentityById("managedIdentityReference")).thenReturn(identity);
        Vault vault = mock(Vault.class);
        when(vault.accessPolicies()).thenReturn(List.of());
        when(azureClient.getKeyVault("resourceGroup", "vaultName")).thenReturn(vault);
        when(azureClient.isValidKeyVaultAccessPolicyListForServicePrincipal(List.of(), "id")).thenReturn(true);
        // WHEN
        underTest.validateEncryptionParameters(validationRequest);
        // THEN
        verify(azurePermissionValidator, never()).validateCMKManagedIdentityPermissions(azureClient, identity, vault);
    }

    @Test
    void tesstValidateEncryptionParametersNoIdentityCloudResource() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        // WHEN
        underTest.validateEncryptionParameters(validationRequest);
        // THEN
        verify(azurePermissionValidator, never()).validateCMKManagedIdentityPermissions(any(), any(), any());
        verify(azureClient, never()).isValidKeyVaultAccessPolicyListForServicePrincipal(any(), any());
    }

    @Test
    void tesstValidateEncryptionParametersNoIdentityOnProvider() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        cloudResourceMap.put(AZURE_MANAGED_IDENTITY, createCloudResource(AZURE_MANAGED_IDENTITY, "managedIdentity", "managedIdentityReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(azureClient.getVaultNameFromEncryptionKeyUrl("keyVaultReference")).thenReturn("vaultName");
        Identity identity = mock(Identity.class);
        when(azureClient.getIdentityById("managedIdentityReference")).thenReturn(null);
        // WHEN
        BadRequestException actualException = assertThrows(BadRequestException.class,
                () -> underTest.validateEncryptionParameters(validationRequest));
        // THEN
        assertTrue(actualException.getMessage().startsWith("Managed identity does not exist"));
    }

    @Test
    void tesstValidateEncryptionParametersNoVault() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        cloudResourceMap.put(AZURE_MANAGED_IDENTITY, createCloudResource(AZURE_MANAGED_IDENTITY, "managedIdentity", "managedIdentityReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(azureClient.getVaultNameFromEncryptionKeyUrl("keyVaultReference")).thenReturn("vaultName");
        Identity identity = mock(Identity.class);
        when(azureClient.getIdentityById("managedIdentityReference")).thenReturn(identity);
        Vault vault = mock(Vault.class);
        when(azureClient.getKeyVault("resourceGroup", "vaultName")).thenReturn(null);
        // WHEN
        BadRequestException actualException = assertThrows(BadRequestException.class,
                () -> underTest.validateEncryptionParameters(validationRequest));
        // THEN
        assertTrue(actualException.getMessage().contains("resource group either does not exist or user does not have permission to access it."));
    }

    @Test
    void tesstValidateEncryptionParametersRoleBasedMissingPermission() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        cloudResourceMap.put(AZURE_MANAGED_IDENTITY, createCloudResource(AZURE_MANAGED_IDENTITY, "managedIdentity", "managedIdentityReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(azureClient.getVaultNameFromEncryptionKeyUrl("keyVaultReference")).thenReturn("vaultName");
        Identity identity = mock(Identity.class);
        when(azureClient.getIdentityById("managedIdentityReference")).thenReturn(identity);
        Vault vault = mock(Vault.class);
        when(vault.roleBasedAccessControlEnabled()).thenReturn(true);
        when(azureClient.getKeyVault("resourceGroup", "vaultName")).thenReturn(vault);
        Throwable expectedException = new RuntimeException("exception");
        doThrow(expectedException).when(azurePermissionValidator).validateCMKManagedIdentityPermissions(azureClient, identity, vault);
        // WHEN
        Throwable actualException = assertThrows(Throwable.class, () -> underTest.validateEncryptionParameters(validationRequest));
        // THEN
        assertEquals(expectedException, actualException);
    }

    @Test
    void tesstValidateEncryptionParametersAccessPoliciesMissingPermission() {
        // GIVEN
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(AZURE_KEYVAULT_KEY, createCloudResource(AZURE_KEYVAULT_KEY, "keyVaultKey", "keyVaultReference"));
        cloudResourceMap.put(AZURE_RESOURCE_GROUP, createCloudResource(AZURE_RESOURCE_GROUP, "resourceGroup", "resourceGroupReference"));
        cloudResourceMap.put(AZURE_MANAGED_IDENTITY, createCloudResource(AZURE_MANAGED_IDENTITY, "managedIdentity", "managedIdentityReference"));
        EncryptionParametersValidationRequest validationRequest = new EncryptionParametersValidationRequest(cloudContext, cloudCredential, cloudResourceMap);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(azureClient.getVaultNameFromEncryptionKeyUrl("keyVaultReference")).thenReturn("vaultName");
        Identity identity = mock(Identity.class);
        when(identity.principalId()).thenReturn("id");
        when(azureClient.getIdentityById("managedIdentityReference")).thenReturn(identity);
        Vault vault = mock(Vault.class);
        when(azureClient.getKeyVault("resourceGroup", "vaultName")).thenReturn(vault);
        when(azureClient.isValidKeyVaultAccessPolicyListForServicePrincipal(List.of(), "id")).thenReturn(false);
        // WHEN
        BadRequestException actualException = assertThrows(BadRequestException.class,
                () -> underTest.validateEncryptionParameters(validationRequest));
        // THEN
        assertTrue(actualException.getMessage().startsWith("Missing Key Vault AccessPolicies (get key, wrap key, unwrap key)"));
    }

    private CloudResource createCloudResource(ResourceType resourceType, String name, String reference) {
        return CloudResource.builder()
                .withType(resourceType)
                .withName(name)
                .withReference(reference)
                .build();
    }
}
