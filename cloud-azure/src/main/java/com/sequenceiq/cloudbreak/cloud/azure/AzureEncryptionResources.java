package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK_ENCRYPTION_SET;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;

@Service
public class AzureEncryptionResources implements EncryptionResources {

    @VisibleForTesting
    static final Pattern RESOURCE_GROUP_NAME = Pattern.compile(".*resourceGroups/([^/]+)/providers.*");

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEncryptionResources.class);

    private static final Pattern ENCRYPTION_KEY_URL_VAULT_NAME = Pattern.compile("https://([^.]+)\\.vault.*");

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

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    @Override
    public CreatedDiskEncryptionSet createDiskEncryptionSet(DiskEncryptionSetCreationRequest diskEncryptionSetCreationRequest) {
        try {
            String vaultName;
            AuthenticatedContext authenticatedContext = azureClientService.createAuthenticatedContext(diskEncryptionSetCreationRequest.getCloudContext(),
                    diskEncryptionSetCreationRequest.getCloudCredential());
            AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);

            Matcher matcher = ENCRYPTION_KEY_URL_VAULT_NAME.matcher(diskEncryptionSetCreationRequest.getEncryptionKeyUrl());
            if (matcher.matches()) {
                vaultName = matcher.group(1);
            } else {
                throw new IllegalArgumentException("vaultName cannot be fetched from encryptionKeyUrl. encryptionKeyUrl should be of format - " +
                        "'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'");
            }
            String vaultResourceGroupName = diskEncryptionSetCreationRequest.getEncryptionKeyResourceGroupName();
            String desResourceGroupName = diskEncryptionSetCreationRequest.getDiskEncryptionSetResourceGroupName();
            String sourceVaultId = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.KeyVault/vaults/%s",
                    azureClient.getCurrentSubscription().subscriptionId(), vaultResourceGroupName, vaultName);

            CreatedDiskEncryptionSet diskEncryptionSet = getOrCreateDiskEncryptionSetOnCloud(
                    authenticatedContext,
                    azureClient,
                    desResourceGroupName,
                    sourceVaultId,
                    diskEncryptionSetCreationRequest);
            // The existence of the DES SP cannot be easily checked. That would need powerful special AD API permissions for the credential app itself that
            // most customers would never grant. Though there is a system assigned managed identity as well sitting behind the SP, querying the properties of
            // this identity (in order to check its existence) would also need an additional powerful Action for the role assigned to the credential app.
            grantKeyVaultAccessPolicyToDiskEncryptionSetServicePrincipal(azureClient, vaultResourceGroupName, vaultName, desResourceGroupName,
                    diskEncryptionSet.getDiskEncryptionSetName(), diskEncryptionSet.getDiskEncryptionSetPrincipalObjectId());
            return diskEncryptionSet;
        } catch (Exception e) {
            LOGGER.error("Disk Encryption Set creation failed, request=" + diskEncryptionSetCreationRequest, e);
            throw azureUtils.convertToCloudConnectorException(e, "Disk Encryption Set creation");
        }
    }

    @Override
    public void deleteDiskEncryptionSet(DiskEncryptionSetDeletionRequest diskEncryptionSetDeletionRequest) {
        try {
            Optional<CloudResource> desCloudResourceOptional =
                    cloudResourceHelper.getResourceTypeFromList(AZURE_DISK_ENCRYPTION_SET, diskEncryptionSetDeletionRequest.getCloudResources());
            if (desCloudResourceOptional.isPresent()) {
                CloudResource desCloudResource = desCloudResourceOptional.get();
                String diskEncryptionSetId = desCloudResource.getReference();
                AzureClient azureClient = azureClientService.getClient(diskEncryptionSetDeletionRequest.getCloudCredential());
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
                persistenceNotifier.notifyDeletion(desCloudResource, diskEncryptionSetDeletionRequest.getCloudContext());
            } else {
                LOGGER.info("No Disk Encryption Set found to delete, request=" + diskEncryptionSetDeletionRequest);
            }
        } catch (Exception e) {
            LOGGER.error("Disk Encryption Set deletion failed, request=" + diskEncryptionSetDeletionRequest, e);
            throw azureUtils.convertToCloudConnectorException(e, "Disk Encryption Set deletion");
        }
    }

    private CreatedDiskEncryptionSet getOrCreateDiskEncryptionSetOnCloud(AuthenticatedContext authenticatedContext, AzureClient azureClient,
            String desResourceGroupName, String sourceVaultId, DiskEncryptionSetCreationRequest diskEncryptionSetCreationRequest) {
        CloudContext cloudContext = diskEncryptionSetCreationRequest.getCloudContext();
        String diskEncryptionSetName = azureUtils.generateDesNameByNameAndId(
                String.format("%s-DES-", cloudContext.getName()), diskEncryptionSetCreationRequest.getId());
        LOGGER.info("Checking if Disk Encryption Set \"{}\" exists", diskEncryptionSetName);
        DiskEncryptionSetInner createdSet = azureClient.getDiskEncryptionSetByName(desResourceGroupName, diskEncryptionSetName);
        if (createdSet == null) {
            LOGGER.info("Creating Disk Encryption Set \"{}\"", diskEncryptionSetName);
            createdSet = azureClient.createDiskEncryptionSet(diskEncryptionSetName, diskEncryptionSetCreationRequest.getEncryptionKeyUrl(),
                    cloudContext.getLocation().getRegion().getRegionName(), desResourceGroupName, sourceVaultId,
                    diskEncryptionSetCreationRequest.getTags());
        } else {
            LOGGER.info("Disk Encryption Set \"{}\" already exists, proceeding with the same", diskEncryptionSetName);
        }
        createdSet = pollDiskEncryptionSetCreation(authenticatedContext, desResourceGroupName, diskEncryptionSetName, createdSet);
        // Neither of createdSet, createdSet.id() or createdSet.identity().principalId() can be null at this point; polling will fail otherwise

        CloudResource desCloudResource = CloudResource.builder()
                .name(diskEncryptionSetName)
                .type(AZURE_DISK_ENCRYPTION_SET)
                .reference(createdSet.id())
                .status(CommonStatus.CREATED)
                .build();
        persistenceNotifier.notifyAllocation(desCloudResource, cloudContext);

        return new CreatedDiskEncryptionSet.Builder()
                .withDiskEncryptionSetId(createdSet.id())
                .withDiskEncryptionSetPrincipalObjectId(createdSet.identity().principalId())
                .withDiskEncryptionSetLocation(createdSet.location())
                .withDiskEncryptionSetName(createdSet.name())
                .withTags(createdSet.getTags())
                .withDiskEncryptionSetResourceGroupName(desResourceGroupName)
                .build();
    }

    private DiskEncryptionSetInner pollDiskEncryptionSetCreation(AuthenticatedContext authenticatedContext, String desResourceGroupName, String desName,
            DiskEncryptionSetInner desInitial) {
        LOGGER.info("Initializing poller for the creation of Disk Encryption Set \"{}\" in Resource Group \"{}\".", desName, desResourceGroupName);
        DiskEncryptionSetCreationCheckerContext checkerContext = new DiskEncryptionSetCreationCheckerContext(desResourceGroupName, desName);
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
        String vaultName;
        String vaultResourceGroupName;
        Matcher matcher = ENCRYPTION_KEY_URL_VAULT_NAME.matcher(encryptionKeyUrl);
        if (matcher.matches()) {
            vaultName = matcher.group(1);
        } else {
            throw new IllegalArgumentException(String.format("Failed to deduce vault name from given encryption key URL \"%s\"", encryptionKeyUrl));
        }
        matcher = RESOURCE_GROUP_NAME.matcher(sourceVaultId);
        if (matcher.matches()) {
            vaultResourceGroupName = matcher.group(1);
        } else {
            throw new IllegalArgumentException(String.format("Failed to deduce vault resource group name from source vault ID \"%s\"", sourceVaultId));
        }
        String description = String.format("access to Key Vault \"%s\" in Resource Group \"%s\" for Service Principal having object ID \"%s\" " +
                        "associated with Disk Encryption Set \"%s\" in Resource Group \"%s\"", vaultName, vaultResourceGroupName, desPrincipalObjectId,
                desName, desResourceGroupName);
        retryService.testWith2SecDelayMax15Times(() -> {
            try {
                LOGGER.info("Removing {}.", description);
                azureClient.removeKeyVaultAccessPolicyFromServicePrincipal(vaultResourceGroupName, vaultName, desPrincipalObjectId);
                LOGGER.info("Removed {}.", description);
                return true;
            } catch (Exception e) {
                throw azureUtils.convertToActionFailedExceptionCausedByCloudConnectorException(e, "Removing " + description);
            }
        });
    }
}