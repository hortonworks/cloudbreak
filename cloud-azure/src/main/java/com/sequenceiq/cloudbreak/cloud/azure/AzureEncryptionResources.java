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

import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureEncryptionResources implements EncryptionResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEncryptionResources.class);

    private static final Pattern ENCRYPTION_KEY_URL_PATTERN = Pattern.compile("https://([^.]+)\\.vault.*");

    private static final Pattern DISK_ENCRYPTION_SET_NAME = Pattern.compile(".*diskEncryptionSets/([^.]+)");

    private static final Pattern DISK_ENCRYPTION_SET_RESOURCE_GROUP = Pattern.compile(".*resourceGroups/([^.]+)/providers.*");

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

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
        String vaultName;
        String vaultResourceGroupName;
        String desResourceGroupName;
        AzureClient azureClient = azureClientService.getClient(diskEncryptionSetCreationRequest.getCloudCredential());

        Matcher matcher = ENCRYPTION_KEY_URL_PATTERN.matcher(diskEncryptionSetCreationRequest.getEncryptionKeyUrl());
        if (matcher.matches()) {
            vaultName = matcher.group(1);
        } else {
            throw new IllegalArgumentException("vaultName cannot be fetched from encryptionKeyUrl. encryptionKeyUrl should be of format - " +
                    "'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'");
        }
        if (diskEncryptionSetCreationRequest.isSingleResourceGroup()) {
            desResourceGroupName = diskEncryptionSetCreationRequest.getResourceGroup();
            vaultResourceGroupName = desResourceGroupName;
        } else {
            throw new IllegalArgumentException("Customer Managed Key Encryption for managed Azure disks is supported only if the CDP resources " +
                    "are in the same resource group as the vault.");
        }
        String sourceVaultId = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.KeyVault/vaults/%s",
                azureClient.getCurrentSubscription().subscriptionId(), vaultResourceGroupName, vaultName);

        CreatedDiskEncryptionSet diskEncryptionSet = getOrCreateDiskEncryptionSetOnCloud(
                azureClient,
                desResourceGroupName,
                sourceVaultId,
                diskEncryptionSetCreationRequest);
        grantKeyVaultAccessPolicyToDiskEncryptionSetServicePrincipal(azureClient, vaultResourceGroupName, vaultName,
                diskEncryptionSet.getDiskEncryptionSetPrincipalId());
        return diskEncryptionSet;
    }

    @Override
    public void deleteDiskEncryptionSet(DiskEncryptionSetDeletionRequest diskEncryptionSetDeletionRequest) {
        Optional<CloudResource> desCloudResourceOptional = diskEncryptionSetDeletionRequest.getCloudResources().stream()
                .filter(r -> r.getType() == ResourceType.AZURE_DISK_ENCRYPTION_SET)
                .findFirst();
        if (desCloudResourceOptional.isPresent()) {
            CloudResource desCloudResource = desCloudResourceOptional.get();
            String diskEncryptionSetId = desCloudResource.getReference();
            CloudCredential cloudCredential = diskEncryptionSetDeletionRequest.getCloudCredential();
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            String diskEncryptionSetName;
            String desResourceGroupName;

            Matcher matcher = DISK_ENCRYPTION_SET_NAME.matcher(diskEncryptionSetId);
            if (matcher.matches()) {
                diskEncryptionSetName = matcher.group(1);
            } else {
                throw new IllegalArgumentException(String.format("Failed to deduce Disk Encryption Set name from given resource id %s",
                        diskEncryptionSetId));
            }
            matcher = DISK_ENCRYPTION_SET_RESOURCE_GROUP.matcher(diskEncryptionSetId);
            if (matcher.matches()) {
                desResourceGroupName = matcher.group(1);
            } else {
                throw new IllegalArgumentException(String.format("Failed to deduce Disk Encryption Set's resource group name from given resource id %s",
                        diskEncryptionSetId));
            }
            LOGGER.info("Deleting Disk Encryption Set {}", diskEncryptionSetId);
            deleteDiskEncryptionSetOnCloud(azureClient, desResourceGroupName, diskEncryptionSetName);
            persistenceNotifier.notifyDeletion(desCloudResource, diskEncryptionSetDeletionRequest.getCloudContext());
        } else {
            LOGGER.info("No Disk Encryption Set found to delete");
        }
    }

    private CreatedDiskEncryptionSet getOrCreateDiskEncryptionSetOnCloud(AzureClient azureClient, String desResourceGroupName,
            String sourceVaultId, DiskEncryptionSetCreationRequest diskEncryptionSetCreationRequest) {
        String diskEncryptionSetName = azureUtils.generateDesNameByNameAndId(
                String.format("%s-DES-", diskEncryptionSetCreationRequest.getEnvironmentName()),
                diskEncryptionSetCreationRequest.getId());
        LOGGER.info("Checking if Disk Encryption Set {} exists on cloud Azure", diskEncryptionSetName);
        DiskEncryptionSetInner createdSet = azureClient.getDiskEncryptionSet(desResourceGroupName, diskEncryptionSetName);
        if (createdSet == null) {
            LOGGER.info("Creating Disk Encryption Set {}", diskEncryptionSetName);
            createdSet = azureClient.createDiskEncryptionSet(diskEncryptionSetName, diskEncryptionSetCreationRequest.getEncryptionKeyUrl(),
                    diskEncryptionSetCreationRequest.getRegion().getRegionName(), desResourceGroupName, sourceVaultId,
                    diskEncryptionSetCreationRequest.getTags());
        } else {
            LOGGER.info("Disk Encryption Set {} exists on cloud Azure, Proceeding with the same", diskEncryptionSetName);
        }
        if (createdSet != null) {
            CloudResource desCloudResource = CloudResource.builder()
                    .name(diskEncryptionSetName)
                    .type(AZURE_DISK_ENCRYPTION_SET)
                    .reference(createdSet.id())
                    .status(CommonStatus.CREATED)
                    .build();
            persistenceNotifier.notifyAllocation(desCloudResource, diskEncryptionSetCreationRequest.getCloudContext());

            return new CreatedDiskEncryptionSet.Builder()
                    .withDiskEncryptionSetId(createdSet.id())
                    .withDiskEncryptionSetPrincipalId(createdSet.identity().principalId())
                    .withDiskEncryptionSetLocation(createdSet.location())
                    .withDiskEncryptionSetName(createdSet.name())
                    .withTags(createdSet.getTags())
                    .withDiskEncryptionSetResourceGroup(desResourceGroupName)
                    .build();
        } else {
            throw new CloudConnectorException("Creating Disk Encryption Set resulted in failure from Azure cloud.");
        }
    }

    private void grantKeyVaultAccessPolicyToDiskEncryptionSetServicePrincipal(AzureClient azureClient, String vaultResourceGroupName, String vaultName,
            String diskEncryptionSetPrincipalId) {
        String description = String.format("access to Key Vault \"%s\"", vaultName);
        retryService.testWith2SecDelayMax15Times(() -> {
            try {
                LOGGER.info("Granting {}.", description);
                azureClient.grantKeyVaultAccessPolicyToServicePrincipal(vaultResourceGroupName, vaultName, diskEncryptionSetPrincipalId);
                LOGGER.info("Granted {}.", description);
                return true;
            } catch (Exception e) {
                LOGGER.warn(String.format("Granting %s failed, %s happened:", description, e.getClass().getName()), e);
                throw Retry.ActionFailedException.ofCause(e);
            }
        });
    }

    private void deleteDiskEncryptionSetOnCloud(AzureClient azureClient, String desResourceGroupName, String diskEncryptionSetName) {
        LOGGER.info("Checking if Disk Encryption Set {} exists on cloud Azure", diskEncryptionSetName);
        DiskEncryptionSetInner existingDiskEncryptionSet = azureClient.getDiskEncryptionSet(desResourceGroupName, diskEncryptionSetName);
        if (existingDiskEncryptionSet != null) {
            azureClient.deleteDiskEncryptionSet(desResourceGroupName, diskEncryptionSetName);
            LOGGER.info("Deleted Disk Encryption Set on Azure cloud");
        } else {
            LOGGER.info("No Disk Encryption Set found to delete");
        }
    }
}