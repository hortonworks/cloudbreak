package com.sequenceiq.cloudbreak.cloud.azure;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccounts;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.storage.SkuTypeResolver;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;

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

    @Value("${cb.azure.image.store.resourcegroup:cloudbreak-images}")
    private String imageStoreResourceGroup;

    @Inject
    private AzureUtils armUtils;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Inject
    private SkuTypeResolver skuTypeResolver;

    public ArmAttachedStorageOption getArmAttachedStorageOption(Map<String, String> parameters) {
        String attachedStorageOption = parameters.get("attachedStorageOption");
        if (Strings.isNullOrEmpty(attachedStorageOption)) {
            return ArmAttachedStorageOption.SINGLE;
        }
        return ArmAttachedStorageOption.valueOf(attachedStorageOption);
    }

    public String getCustomImageId(AzureClient client, AuthenticatedContext ac, CloudStack stack) {
        return getCustomImageId(client, ac, stack, stack.getImage().getImageName());
    }

    public String getCustomImageId(AzureClient client, AuthenticatedContext ac, CloudStack stack, String imageName) {
        String imageResourceGroupName = getImageResourceGroupName(ac.getCloudContext(), stack);
        AzureCredentialView acv = new AzureCredentialView(ac.getCloudCredential());
        String imageStorageName = getImageStorageName(acv, ac.getCloudContext(), stack);
        String imageBlobUri = client.getImageBlobUri(imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, imageName);
        String region = ac.getCloudContext().getLocation().getRegion().value();
        return getCustomImageId(imageBlobUri, imageResourceGroupName, region, client);
    }

    private String getCustomImageId(String vhd, String imageResourceGroupName, String region, AzureClient client) {
        String customImageId = client.getCustomImageId(imageResourceGroupName, vhd, region);
        LOGGER.debug("Custom image id: {}", customImageId);
        return customImageId;
    }

    public String getImageStorageName(AzureCredentialView acv, CloudContext cloudContext, CloudStack stack) {
        String storageName;
        if (isLegacyImageStore(stack)) {
            String persistentStorageName = getPersistentStorageName(stack);
            storageName = isPersistentStorage(persistentStorageName)
                    ? getPersistentStorageName(persistentStorageName, acv, cloudContext.getLocation().getRegion().value())
                    : buildStorageName(getArmAttachedStorageOption(stack.getParameters()), acv, null, cloudContext, AzureDiskType.LOCALLY_REDUNDANT);
        } else {
            storageName = getPersistentStorageName(imageStorePrefix, acv, cloudContext.getLocation().getRegion().value());
        }
        return storageName;
    }

    private boolean isLegacyImageStore(CloudStack stack) {
        return stack.getParameters().get("legacyImageStore") != null;
    }

    public String getAttachedDiskStorageName(ArmAttachedStorageOption armAttachedStorageOption, AzureCredentialView acv, Long vmId, CloudContext cloudContext,
            AzureDiskType storageType) {
        return buildStorageName(armAttachedStorageOption, acv, vmId, cloudContext, storageType);
    }

    public void createStorage(AzureClient client, String osStorageName, AzureDiskType storageType, String storageGroup, String region, Boolean encrypted,
            Map<String, String> tags)
            throws CloudException {
        if (!storageAccountExist(client, osStorageName)) {
            client.createStorageAccount(storageGroup, osStorageName, region, skuTypeResolver.resolveFromAzureDiskType(storageType), encrypted, tags,
                    defaultCostTaggingService.prepareStorageTagging());
        }
    }

    public void deleteStorage(AzureClient client, String osStorageName, String storageGroup)
            throws CloudException {
        if (storageAccountExist(client, osStorageName)) {
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
            String storageAccountId = acv.getId() + "#" + cloudContext.getId() + '#' + cloudContext.getWorkspaceId();
            LOGGER.debug("Storage account internal id: {}", storageAccountId);
            byte[] digest = messageDigest.digest(storageAccountId.getBytes());
            String paddedId = "";
            if (armAttachedStorageOption == ArmAttachedStorageOption.PER_VM && vmId != null) {
                paddedId = String.format("%3s", Long.toString(vmId, RADIX)).replace(' ', '0');
            }
            result = name + storageType.getAbbreviation() + paddedId + new BigInteger(1, digest).toString(RADIX);
        } catch (NoSuchAlgorithmException ignored) {
            result = name + acv.getId() + cloudContext.getId() + cloudContext.getWorkspaceId();
        }
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME);
        }
        LOGGER.debug("Storage account name: {}", result);
        return result;
    }

    private String getPersistentStorageName(String persistentStorageName, AzureCredentialView acv, String region) {
        String subscriptionIdPart = acv.getSubscriptionId().replaceAll("-", "").toLowerCase();
        String regionInitials = WordUtils.initials(region, ' ').toLowerCase();
        String result = String.format("%s%s%s", persistentStorageName, regionInitials, subscriptionIdPart);
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

    public String getImageResourceGroupName(CloudContext cloudContext, CloudStack stack) {
        if (isLegacyImageStore(stack)) {
            if (isPersistentStorage(getPersistentStorageName(stack))) {
                return getPersistentStorageName(stack);
            }
            return armUtils.getResourceGroupName(cloudContext, stack);
        } else {
            return imageStoreResourceGroup;
        }
    }

    private boolean storageAccountExist(AzureClient client, String storageName) {
        try {
            StorageAccounts storageAccounts = client.getStorageAccounts();
            for (StorageAccount account : storageAccounts.list()) {
                if (account.name().equals(storageName)) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }
}


