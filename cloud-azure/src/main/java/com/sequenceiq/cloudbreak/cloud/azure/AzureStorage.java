package com.sequenceiq.cloudbreak.cloud.azure;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccounts;
import com.microsoft.azure.management.storage.implementation.StorageAccountInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureStorageAccountBuilderService;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.StorageAccountParameters;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfoService;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageService;
import com.sequenceiq.cloudbreak.cloud.azure.storage.SkuTypeResolver;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Service
public class AzureStorage {

    public static final String IMAGES_CONTAINER = "images";

    public static final String STORAGE_BLOB_PATTERN = "https://%s.blob.core.windows.net/";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorage.class);

    private static final int RADIX = 32;

    private static final int MAX_LENGTH_OF_NAME_SLICE = 8;

    private static final int MAX_LENGTH_OF_RESOURCE_NAME = 24;

    @Value("${cb.azure.image.store.prefix:cbimg}")
    private String imageStorePrefix;

    @Inject
    private AzureUtils armUtils;

    @Inject
    private SkuTypeResolver skuTypeResolver;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureStorageAccountBuilderService azureStorageAccountBuilderService;

    @Inject
    private AzureImageService azureImageService;

    @Inject
    private AzureImageInfoService azureImageInfoService;

    public ArmAttachedStorageOption getArmAttachedStorageOption(Map<String, String> parameters) {
        String attachedStorageOption = parameters.get("attachedStorageOption");
        if (Strings.isNullOrEmpty(attachedStorageOption)) {
            return ArmAttachedStorageOption.SINGLE;
        }
        return ArmAttachedStorageOption.valueOf(attachedStorageOption);
    }

    public AzureImage getCustomImage(AzureClient client, AuthenticatedContext ac, CloudStack stack) {
        return getCustomImage(client, ac, stack, stack.getImage().getImageName());
    }

    public AzureImage getCustomImage(AzureClient client, AuthenticatedContext ac, CloudStack stack, String imageName) {
        String imageResourceGroupName = azureResourceGroupMetadataProvider.getImageResourceGroupName(ac.getCloudContext(), stack);
        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = getImageStorageName(acv, ac.getCloudContext(), stack);

        AzureImageInfo azureImageInfo = azureImageInfoService.getImageInfo(imageResourceGroupName, imageName, ac, client);
        Optional<AzureImage> foundImage = azureImageService.findImage(azureImageInfo, client, ac);
        if (foundImage.isPresent()) {
            LOGGER.info("Custom image with id {} already exists in the target resource group {}, bypassing VHD check!",
                    foundImage.get().getId(), imageResourceGroupName);
            return foundImage.get();
        }

        String imageBlobUri = client.getImageBlobUri(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, azureImageInfo.getImageName());
        AzureImage createdImage = azureImageService.createImage(azureImageInfo, imageBlobUri, client, ac);
        LOGGER.debug("Custom image id: {}", createdImage.getId());
        return createdImage;
    }

    public String getImageStorageName(AzureCredentialView acv, CloudContext cloudContext, CloudStack cloudStack) {
        if (azureResourceGroupMetadataProvider.useSingleResourceGroup(cloudStack)) {
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack);
            return getPersistentStorageName(imageStorePrefix, acv, cloudContext.getLocation().getRegion().value(), resourceGroupName);
        } else {
            return getPersistentStorageName(imageStorePrefix, acv, cloudContext.getLocation().getRegion().value(), null);
        }
    }

    public String getAttachedDiskStorageName(ArmAttachedStorageOption armAttachedStorageOption, AzureCredentialView acv, Long vmId, CloudContext cloudContext,
            AzureDiskType storageType) {
        return buildStorageName(armAttachedStorageOption, acv, vmId, cloudContext, storageType);
    }

    public StorageAccount createStorage(AzureClient client, String osStorageName, AzureDiskType storageType, String storageGroup,
            String region, Boolean encrypted, Map<String, String> tags) throws CloudException {
        Optional<StorageAccount> storageAccountOptional = findStorageAccount(client, osStorageName);
        if (storageAccountOptional.isEmpty()) {
            StorageAccountParameters storageAccountParameters = new StorageAccountParameters(
                    storageGroup, osStorageName, region, skuTypeResolver.resolveFromAzureDiskType(storageType), encrypted, tags);
            return azureStorageAccountBuilderService.buildStorageAccount(client, storageAccountParameters);
        } else {
            StorageAccount storageAccount = storageAccountOptional.get();
            String errorMessage = String.format("Storage account creation is not possible "
                    + "as there is already a storage account with name %s in the resource group %s "
                    + "in your subscription and the name must be unique across Azure. "
                    + "In order to proceed, please delete that storage account.", storageAccount.name(), storageAccount.resourceGroupName());
            LOGGER.warn(errorMessage);
            throw new CloudbreakServiceException(errorMessage);
        }
    }

    public void deleteStorage(AzureClient client, String osStorageName, String storageGroup)
            throws CloudException {
        Optional<StorageAccount> storageAccountOptional = findStorageAccount(client, osStorageName);
        if (storageAccountOptional.isPresent()) {
            client.deleteStorageAccount(storageGroup, osStorageName);
        }
    }

    private String buildStorageName(ArmAttachedStorageOption armAttachedStorageOption, AzureCredentialView acv, Long vmId, CloudContext cloudContext,
            AzureDiskType storageType) {
        String result;
        String name = cloudContext.getName().toLowerCase().replaceAll("\\s+|-", "");
        name = name.length() > MAX_LENGTH_OF_NAME_SLICE ? name.substring(0, MAX_LENGTH_OF_NAME_SLICE) : name;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            String storageAccountId = acv.getCredentialCrn() + "#" + cloudContext.getId() + '#' + cloudContext.getAccountId();
            LOGGER.debug("Storage account internal id: {}", storageAccountId);
            byte[] digest = messageDigest.digest(storageAccountId.getBytes());
            String paddedId = "";
            if (armAttachedStorageOption == ArmAttachedStorageOption.PER_VM && vmId != null) {
                paddedId = String.format("%3s", Long.toString(vmId, RADIX)).replace(' ', '0');
            }
            result = name + storageType.getAbbreviation() + paddedId + new BigInteger(1, digest).toString(RADIX);
        } catch (NoSuchAlgorithmException ignored) {
            result = name + acv.getCredentialCrn() + cloudContext.getId() + cloudContext.getAccountId();
        }
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME);
        }
        LOGGER.debug("Storage account name: {}", result);
        return result;
    }

    private String getPersistentStorageName(String prefix, AzureCredentialView acv, String region, String resourceGroup) {
        String subscriptionIdPart = StringUtils.isBlank(resourceGroup)
                ? acv.getSubscriptionId().replaceAll("-", "").toLowerCase()
                : armUtils.encodeString(acv.getSubscriptionId().replaceAll("-", "").toLowerCase());
        String regionInitials = WordUtils.initials(Region.findByLabelOrName(region).label(), ' ').toLowerCase();
        String resourceGroupPart = armUtils.encodeString(resourceGroup);
        String result = String.format("%s%s%s%s", prefix, regionInitials, subscriptionIdPart, resourceGroupPart);
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME);
        }
        LOGGER.debug("Storage account name: {}", result);
        return result;
    }

    public String getDiskContainerName(CloudContext cloudContext) {
        return armUtils.getStackName(cloudContext);
    }

    public String getPersistentStorageName(CloudStack stack) {
        return stack.getParameters().get("persistentStorage");
    }

    public Boolean isEncrytionNeeded(Map<String, String> parameters) {
        return Boolean.parseBoolean(parameters.get("encryptStorage"));
    }

    public boolean isPersistentStorage(String persistentStorageName) {
        return !Strings.isNullOrEmpty(persistentStorageName);
    }

    private Optional<StorageAccount> findStorageAccount(AzureClient client, String storageName) {
        try {
            StorageAccounts storageAccounts = client.getStorageAccounts();
            for (StorageAccount account : storageAccounts.list()) {
                if (account.name().equals(storageName)) {
                    return Optional.of(account);
                }
            }
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public Optional<String> findStorageAccountIdInVisibleSubscriptions(AzureClient client, String account) {
        Optional<StorageAccount> storageAccount = client.getStorageAccount(account, Kind.STORAGE_V2);
        LOGGER.debug("checking current subscription for storage account");
        if (storageAccount.isPresent()) {
            return storageAccount.map(HasId::id);
        }

        List<String> subscriptionIds = client.listSubscriptions().stream().map(Subscription::subscriptionId).collect(Collectors.toList());
        LOGGER.debug("Checking other subscriptions for storage account: {}", String.join(",", subscriptionIds));
        for (String subscriptionId : subscriptionIds) {
            Optional<StorageAccountInner> storageAccountInner = client.getStorageAccountBySubscription(account, subscriptionId, Kind.STORAGE_V2);
            if (storageAccountInner.isPresent()) {
                return storageAccountInner.map(s -> s.id());
            }
        }

        return Optional.empty();
    }

}


