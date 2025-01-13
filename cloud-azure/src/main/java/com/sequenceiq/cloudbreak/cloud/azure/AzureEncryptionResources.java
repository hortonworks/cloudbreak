package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK_ENCRYPTION_SET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.compute.fluent.models.DiskEncryptionSetInner;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.msi.models.Identity;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationPoller;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureEncryptionResources implements EncryptionResources {

    @VisibleForTesting
    static final Pattern RESOURCE_GROUP_NAME = Pattern.compile(".*resourceGroups/([^/]+)/providers.*");

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEncryptionResources.class);

    private static final Pattern DISK_ENCRYPTION_SET_NAME = Pattern.compile(".*diskEncryptionSets/([^.]+)");

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private DiskEncryptionSetCreationPoller diskEncryptionSetCreationPoller;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AzurePermissionValidator azurePermissionValidator;

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    @Override
    public void validateEncryptionParameters(EncryptionParametersValidationRequest validationRequest) {
        AuthenticatedContext authenticatedContext = azureClientService.createAuthenticatedContext(validationRequest.cloudContext(),
                validationRequest.cloudCredential());
        AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);

        Map<ResourceType, CloudResource> cloudResourceMap = validationRequest.cloudResources();
        CloudResource vaultKeyResource = cloudResourceMap.get(ResourceType.AZURE_KEYVAULT_KEY);
        CloudResource vaultResourceGroup = cloudResourceMap.get(ResourceType.AZURE_RESOURCE_GROUP);
        CloudResource managedIdentity = cloudResourceMap.get(ResourceType.AZURE_MANAGED_IDENTITY);
        if (vaultResourceGroup == null || vaultKeyResource == null) {
            throw new BadRequestException("Vault key and vault resource group is mandatory parameters");
        }
        String vaultResourceGroupName = vaultResourceGroup.getName();
        String vaultName = azureClient.getVaultNameFromEncryptionKeyUrl(vaultKeyResource.getReference());
        if (managedIdentity != null) {
            try {
                Identity identity = azureClient.getIdentityById(managedIdentity.getReference());
                if (identity == null) {
                    String identityNotFound = String.format("Managed identity does not exist: %s. Please fix the reference or " +
                            "create Managed identity as it is mandatory for CMK and retry the operation", managedIdentity.getReference());

                    LOGGER.error(identityNotFound);
                    throw new BadRequestException(identityNotFound);
                }
                Vault vault = azureClient.getKeyVault(vaultResourceGroupName, vaultName);
                if (vault == null) {
                    String vaultNotFound = String.format("Vault with name \"%s\" in \"%s\" resource group either does not exist or user does not have " +
                            "permission to access it. Kindly check if the vault & encryption key exists and the correct encryption key URL is specified.",
                            vaultName, vaultResourceGroupName);
                    LOGGER.error(vaultNotFound);
                    throw new BadRequestException(vaultNotFound);
                } else if (vault.roleBasedAccessControlEnabled()) {
                    azurePermissionValidator.validateCMKManagedIdentityPermissions(azureClient, identity, vault);
                } else if (!azureClient.isValidKeyVaultAccessPolicyListForServicePrincipal(vault.accessPolicies(), identity.principalId())) {
                    String missingAccessPolicies = String.format(
                            "Missing Key Vault AccessPolicies (get key, wrap key, unwrap key) in %s key vault for %s managed identity",
                            vaultName, managedIdentity.getName());
                    LOGGER.error(missingAccessPolicies);
                    throw new BadRequestException(missingAccessPolicies);
                }
            } catch (ProviderAuthenticationFailedException authenticationException) {
                LOGGER.error("CMK permission checking is not authorized on your credential: {}", authenticationException.getMessage());
                throw new BadRequestException(String.format("User does not have read permissions to Vault with name \"%s\" in \"%s\" resourgroup. " +
                        " Kindly check if the user has read permission for the Vault.", vaultName, vaultResourceGroupName));
            }
        } else {
            LOGGER.info("No managed identity is given, CMK validation will be skipped");
        }
    }

    @Override
    public CreatedDiskEncryptionSet createDiskEncryptionSet(DiskEncryptionSetCreationRequest request) {
        try {
            AuthenticatedContext authenticatedContext = azureClientService.createAuthenticatedContext(request.getCloudContext(),
                    request.getCloudCredential());
            AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);

            String vaultName = azureClient.getVaultNameFromEncryptionKeyUrl(request.getEncryptionKeyUrl());
            if (vaultName == null) {
                throw new IllegalArgumentException("vaultName cannot be fetched from encryptionKeyUrl. encryptionKeyUrl should be of format - " +
                        "'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'");
            }
            boolean singleResourceGroup = Boolean.TRUE;
            String vaultResourceGroupName = request.getEncryptionKeyResourceGroupName();
            String desResourceGroupName = request.getDiskEncryptionSetResourceGroupName();

            if (StringUtils.isEmpty(desResourceGroupName)) {
                if (StringUtils.isEmpty(vaultResourceGroupName)) {
                    throw new IllegalArgumentException("Encryption key resource group name should be present if resource group is not provided during " +
                            "environment creation. At least one of --resource-group-name or --encryption-key-resource-group-name should be specified.");
                }
                singleResourceGroup = Boolean.FALSE;
                desResourceGroupName = azureUtils.generateResourceNameByNameAndId(
                        String.format("%s-CDP_DES-", request.getCloudContext().getName()),
                        request.getId());
            }

            String sourceVaultId = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.KeyVault/vaults/%s",
                    azureClient.getCurrentSubscription().subscriptionId(), vaultResourceGroupName, vaultName);

            // Check for the existence of keyVault user has specified before creating disk encryption set (DES).
            // If keyVault is wrong or user lacks permissions to access it, granting access permissions to this keyVault for DES would fail.
            if (!azureClient.keyVaultExists(vaultResourceGroupName, vaultName)) {
                throw new IllegalArgumentException(String.format("Vault with name \"%s\" either does not exist or user does not have permissions to " +
                                "access it. Kindly check if the vault & encryption key exists and correct encryption key URL is specified.",
                        vaultName));
            }
            CreatedDiskEncryptionSet diskEncryptionSet = getOrCreateDiskEncryptionSetOnCloud(
                    authenticatedContext,
                    azureClient,
                    desResourceGroupName,
                    sourceVaultId,
                    request,
                    singleResourceGroup);
            // The existence of the DES SP cannot be easily checked. That would need powerful special AD API permissions for the credential app itself that
            // most customers would never grant. Though there is a system assigned managed identity as well sitting behind the SP, querying the properties of
            // this identity (in order to check its existence) would also need an additional powerful Action for the role assigned to the credential app.
            if (request.getUserManagedIdentity().isEmpty()) {
                grantKeyVaultAccessPolicyToDiskEncryptionSetServicePrincipal(azureClient,
                        vaultResourceGroupName,
                        vaultName,
                        desResourceGroupName,
                        diskEncryptionSet.getDiskEncryptionSetName(),
                        diskEncryptionSet.getDiskEncryptionSetPrincipalObjectId());
            }
            return diskEncryptionSet;
        } catch (Exception e) {
            LOGGER.error("Disk Encryption Set creation failed, request=" + request, e);
            throw azureUtils.convertToCloudConnectorException(e, "Disk Encryption Set creation");
        }
    }

    @Override
    public void deleteDiskEncryptionSet(DiskEncryptionSetDeletionRequest request) {
        try {
            Optional<CloudResource> desCloudResourceOptional =
                    cloudResourceHelper.getResourceTypeFromList(AZURE_DISK_ENCRYPTION_SET, request.getCloudResources());
            Optional<CloudResource> rgCloudResourceOptional =
                    cloudResourceHelper.getResourceTypeFromList(AZURE_RESOURCE_GROUP, request.getCloudResources());
            if (desCloudResourceOptional.isPresent()) {
                AzureClient azureClient = azureClientService.getClient(request.getCloudCredential());
                CloudResource desCloudResource = desCloudResourceOptional.get();
                String diskEncryptionSetId = desCloudResource.getReference();
                String diskEncryptionSetName;
                String desResourceGroupName;

                Matcher matcher = DISK_ENCRYPTION_SET_NAME.matcher(diskEncryptionSetId);
                if (matcher.matches()) {
                    diskEncryptionSetName = matcher.group(1);
                } else {
                    throw new IllegalArgumentException(String.format("Failed to deduce Disk Encryption Set name from given resource id \"%s\"",
                            diskEncryptionSetId));
                }
                matcher = RESOURCE_GROUP_NAME.matcher(diskEncryptionSetId);
                if (matcher.matches()) {
                    desResourceGroupName = matcher.group(1);
                } else {
                    throw new IllegalArgumentException(String.format("Failed to deduce Disk Encryption Set's resource group name from given resource id \"%s\"",
                            diskEncryptionSetId));
                }
                LOGGER.info("Deleting Disk Encryption Set \"{}\"", diskEncryptionSetId);
                deleteDiskEncryptionSetOnCloud(azureClient, desResourceGroupName, diskEncryptionSetName);
                persistenceNotifier.notifyDeletion(desCloudResource, request.getCloudContext());
            } else {
                LOGGER.info("No Disk Encryption Set found to delete, request=" + request);
            }
            if (rgCloudResourceOptional.isPresent()) {
                CloudResource rgCloudResource = rgCloudResourceOptional.get();
                checkAndDeleteDesResourceGroupByName(azureClientService.getClient(request.getCloudCredential()),
                        rgCloudResource.getName());
                persistenceNotifier.notifyDeletion(rgCloudResource, request.getCloudContext());
            } else {
                LOGGER.info("No resource group is found to delete, request=" + request);
            }
        } catch (Exception e) {
            LOGGER.error("Disk Encryption Set deletion failed, request=" + request, e);
            throw azureUtils.convertToCloudConnectorException(e, "Disk Encryption Set deletion");
        }
    }

    private void checkAndDeleteDesResourceGroupByName(AzureClient azureClient, String desResourceGroupName) {
        if (azureClient.resourceGroupExists(desResourceGroupName)) {
            LOGGER.info("Deleting resource group \"{}\".", desResourceGroupName);
            azureClient.deleteResourceGroup(desResourceGroupName);
        } else {
            LOGGER.info("Deletion of resource group \"{}\" is not required.", desResourceGroupName);
        }
    }

    private void checkAndCreateDesResourceGroupByName(CloudContext cloudContext, AzureClient azureClient, String desResourceGroupName, String region,
            Map<String, String> tags) {
        ResourceGroup resourceGroup;
        if (azureClient.resourceGroupExists(desResourceGroupName)) {
            LOGGER.info("Resource group \"{}\" already exists, using it for creating disk encryption set.", desResourceGroupName);
            resourceGroup = azureClient.getResourceGroup(desResourceGroupName);
        } else {
            LOGGER.info("Creating resource group \"{}\" for disk encryption set.", desResourceGroupName);
            resourceGroup = azureClient.createResourceGroup(desResourceGroupName, region, tags);
        }
        // Resource group is persisted in cloudResource only when it is created by CDP, as part of disk encryption set creation in case of
        // multi-resource group.
        CloudResource rgCloudResource = CloudResource.builder()
                .withName(desResourceGroupName)
                .withType(AZURE_RESOURCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withReference(resourceGroup.id())
                .build();
        persistenceNotifier.notifyAllocation(rgCloudResource, cloudContext);
    }

    private CreatedDiskEncryptionSet getOrCreateDiskEncryptionSetOnCloud(AuthenticatedContext authenticatedContext, AzureClient azureClient,
            String desResourceGroupName, String sourceVaultId, DiskEncryptionSetCreationRequest request, boolean singleResourceGroup) {
        CloudContext cloudContext = request.getCloudContext();
        String region = cloudContext.getLocation().getRegion().getRegionName();
        Map<String, String> tags = request.getTags();
        String diskEncryptionSetName = azureUtils.generateDesNameByNameAndId(
                String.format("%s-DES-", cloudContext.getName()), request.getId());
        LOGGER.info("Checking if Disk Encryption Set \"{}\" exists", diskEncryptionSetName);
        DiskEncryptionSetInner createdSet = azureClient.getDiskEncryptionSetByName(desResourceGroupName, diskEncryptionSetName);
        if (createdSet == null) {
            if (!singleResourceGroup) {
                LOGGER.info("Check and create resource group \"{}\" for disk encryption set", desResourceGroupName);
                checkAndCreateDesResourceGroupByName(cloudContext, azureClient, desResourceGroupName, region, tags);
            }
            LOGGER.info("Creating Disk Encryption Set \"{}\" in resource group \"{}\"", diskEncryptionSetName, desResourceGroupName);
            createdSet = azureClient.createDiskEncryptionSet(diskEncryptionSetName,
                    request.getUserManagedIdentity(),
                    request.getEncryptionKeyUrl(),
                    region,
                    desResourceGroupName,
                    sourceVaultId,
                    tags);
        } else {
            LOGGER.info("Disk Encryption Set \"{}\" already exists, proceeding with the same", diskEncryptionSetName);
        }
        createdSet = pollDiskEncryptionSetCreation(
                authenticatedContext,
                desResourceGroupName,
                diskEncryptionSetName,
                createdSet,
                request.getUserManagedIdentity().isPresent());
        // Neither of createdSet, createdSet.id() or createdSet.identity().principalId() can be null at this point; polling will fail otherwise

        CloudResource desCloudResource = CloudResource.builder()
                .withName(diskEncryptionSetName)
                .withType(AZURE_DISK_ENCRYPTION_SET)
                .withReference(createdSet.id())
                .withStatus(CommonStatus.CREATED)
                .build();
        persistenceNotifier.notifyAllocation(desCloudResource, cloudContext);

        return new CreatedDiskEncryptionSet.Builder()
                .withDiskEncryptionSetId(createdSet.id())
                .withDiskEncryptionSetPrincipalObjectId(createdSet.identity().principalId())
                .withDiskEncryptionSetLocation(createdSet.location())
                .withDiskEncryptionSetName(createdSet.name())
                .withTags(createdSet.tags())
                .withDiskEncryptionSetResourceGroupName(desResourceGroupName)
                .build();
    }

    private DiskEncryptionSetInner pollDiskEncryptionSetCreation(AuthenticatedContext authenticatedContext, String desResourceGroupName, String desName,
            DiskEncryptionSetInner desInitial, boolean userManagedIdentityEnabled) {
        LOGGER.info("Initializing poller for the creation of Disk Encryption Set \"{}\" in Resource Group \"{}\".", desName, desResourceGroupName);
        DiskEncryptionSetCreationCheckerContext checkerContext = new DiskEncryptionSetCreationCheckerContext(
                desResourceGroupName,
                desName,
                userManagedIdentityEnabled);
        return diskEncryptionSetCreationPoller.startPolling(authenticatedContext, checkerContext, desInitial);
    }

    private void grantKeyVaultAccessPolicyToDiskEncryptionSetServicePrincipal(AzureClient azureClient, String vaultResourceGroupName, String vaultName,
            String desResourceGroupName, String desName, String desPrincipalObjectId) {
        String description = String.format("access to Key Vault \"%s\" in Resource Group \"%s\" for Service Principal having object ID \"%s\" " +
                        "associated with Disk Encryption Set \"%s\" in Resource Group \"%s\"", vaultName, vaultResourceGroupName, desPrincipalObjectId,
                desName, desResourceGroupName);
        retryService.testWith2SecDelayMax15Times(() -> {
            try {
                LOGGER.info("Granting {}.", description);
                azureClient.grantKeyVaultAccessPolicyToServicePrincipal(vaultResourceGroupName, vaultName, desPrincipalObjectId);
                if (!azureClient.isValidKeyVaultAccessPolicyListForServicePrincipal(vaultResourceGroupName, vaultName, desPrincipalObjectId)) {
                    throw new CloudConnectorException(
                            String.format("Access policy has not been granted to object Id: %s, Retrying ...", desPrincipalObjectId));
                }
                LOGGER.info("Granted {}.", description);
                return true;
            } catch (Exception e) {
                throw azureUtils.convertToActionFailedExceptionCausedByCloudConnectorException(e, "Granting " + description);
            }
        });
    }

    private void deleteDiskEncryptionSetOnCloud(AzureClient azureClient, String desResourceGroupName, String desName) {
        String description = String.format("Disk Encryption Set \"%s\" in Resource Group \"%s\"", desName, desResourceGroupName);
        retryService.testWith2SecDelayMax15Times(() -> {
            try {
                LOGGER.info("Checking if {} exists.", description);
                DiskEncryptionSetInner existingDiskEncryptionSet = azureClient.getDiskEncryptionSetByName(desResourceGroupName, desName);
                if (existingDiskEncryptionSet != null) {
                    LOGGER.info("Deleting {}.", description);
                    azureClient.deleteDiskEncryptionSet(desResourceGroupName, desName);
                    LOGGER.info("Deleted {}.", description);
                    removeKeyVaultAccessPolicyFromDiskEncryptionSetServicePrincipal(azureClient, desResourceGroupName, desName,
                            existingDiskEncryptionSet.activeKey().keyUrl(), existingDiskEncryptionSet.identity().principalId(),
                            existingDiskEncryptionSet.activeKey().sourceVault().id());
                } else {
                    LOGGER.info("No {} found to delete.", description);
                }
                return true;
            } catch (Exception e) {
                throw azureUtils.convertToActionFailedExceptionCausedByCloudConnectorException(e, "Deletion of " + description);
            }
        });
    }

    private void removeKeyVaultAccessPolicyFromDiskEncryptionSetServicePrincipal(AzureClient azureClient, String desResourceGroupName, String desName,
            String encryptionKeyUrl, String desPrincipalObjectId, String sourceVaultId) {
        String vaultResourceGroupName;
        String vaultName = azureClient.getVaultNameFromEncryptionKeyUrl(encryptionKeyUrl);
        if (vaultName == null) {
            throw new IllegalArgumentException(String.format("Failed to deduce vault name from given encryption key URL \"%s\"", encryptionKeyUrl));
        }
        Matcher matcher = RESOURCE_GROUP_NAME.matcher(sourceVaultId);
        if (matcher.matches()) {
            vaultResourceGroupName = matcher.group(1);
        } else {
            throw new IllegalArgumentException(String.format("Failed to deduce vault resource group name from source vault ID \"%s\"", sourceVaultId));
        }

        // Check for the existence of keyVault user has specified before removing disk encryption set's (DES) access permissions from this keyVault.
        if (!azureClient.keyVaultExists(vaultResourceGroupName, vaultName)) {
            LOGGER.warn(String.format("Vault with name \"%s\" either does not exist/have been deleted or user does not have permissions to access it.",
                    vaultName));
        } else {
            String description = String.format("access to Key Vault \"%s\" in Resource Group \"%s\" for Service Principal having object ID \"%s\" " +
                            "associated with Disk Encryption Set \"%s\" in Resource Group \"%s\"", vaultName, vaultResourceGroupName, desPrincipalObjectId,
                    desName, desResourceGroupName);
            retryService.testWith2SecDelayMax15Times(() -> {
                try {
                    LOGGER.info("Removing {}.", description);
                    azureClient.removeKeyVaultAccessPolicyForServicePrincipal(vaultResourceGroupName, vaultName, desPrincipalObjectId);
                    LOGGER.info("Removed {}.", description);
                    return true;
                } catch (Exception e) {
                    throw azureUtils.convertToActionFailedExceptionCausedByCloudConnectorException(e, "Removing " + description);
                }
            });
        }
    }
}