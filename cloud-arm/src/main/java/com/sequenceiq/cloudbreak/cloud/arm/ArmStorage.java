package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus.NOTFOUND;
import static com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus.SUCCEEDED;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class ArmStorage {

    public static final String IMAGES = "images";
    public static final String STORAGE_BLOB_PATTERN = "https://%s.blob.core.windows.net/";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmStorage.class);

    private static final int RADIX = 32;
    private static final int MAX_LENGTH_OF_NAME_SLICE = 8;
    private static final int MAX_LENGTH_OF_RESOURCE_NAME = 24;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private ArmPollTaskFactory armPollTaskFactory;
    @Inject
    private ArmUtils armUtils;

    public ArmAttachedStorageOption getArmAttachedStorageOption(Map<String, String> parameters) {
        String attachedStorageOption = parameters.get("attachedStorageOption");
        if (Strings.isNullOrEmpty(attachedStorageOption)) {
            return ArmAttachedStorageOption.SINGLE;
        }
        return ArmAttachedStorageOption.valueOf(attachedStorageOption);
    }

    public String getImageStorageName(ArmCredentialView acv, CloudContext cloudContext, String persistentStorageName,
            ArmAttachedStorageOption armAttachedStorageOption) {
        String storageName;
        if (isPersistentStorage(persistentStorageName)) {
            storageName = getPersistentStorageName(persistentStorageName, acv, cloudContext.getLocation().getRegion().value());
        } else {
            storageName = buildStorageName(armAttachedStorageOption, acv, null, cloudContext, ArmDiskType.LOCALLY_REDUNDANT);
        }
        return storageName;
    }

    public String getAttachedDiskStorageName(ArmAttachedStorageOption armAttachedStorageOption, ArmCredentialView acv, Long vmId, CloudContext cloudContext,
            ArmDiskType storageType) {
        return buildStorageName(armAttachedStorageOption, acv, vmId, cloudContext, storageType);
    }

    public void createStorage(AuthenticatedContext ac, AzureRMClient client, String osStorageName, ArmDiskType storageType, String storageGroup, String region)
            throws Exception {
        if (!storageAccountExist(client, osStorageName)) {
            client.createStorageAccount(storageGroup, osStorageName, region, storageType.value());
            PollTask<Boolean> task = armPollTaskFactory.newStorageStatusCheckerTask(ac,
                    new StorageCheckerContext(new ArmCredentialView(ac.getCloudCredential()), storageGroup, osStorageName, SUCCEEDED));
            syncPollingScheduler.schedule(task);
        }
    }

    public void deleteStorage(AuthenticatedContext authenticatedContext, AzureRMClient client, String osStorageName, String storageGroup)
            throws Exception {
        if (storageAccountExist(client, osStorageName)) {
            client.deleteStorageAccount(storageGroup, osStorageName);
            PollTask<Boolean> task = armPollTaskFactory.newStorageStatusCheckerTask(authenticatedContext,
                    new StorageCheckerContext(new ArmCredentialView(authenticatedContext.getCloudCredential()), storageGroup, osStorageName, NOTFOUND));
            syncPollingScheduler.schedule(task);
        }
    }

    private String buildStorageName(ArmAttachedStorageOption armAttachedStorageOption, ArmCredentialView acv, Long vmId, CloudContext cloudContext,
                ArmDiskType storageType) {
        String result;
        String name = cloudContext.getName().toLowerCase().replaceAll("\\s+|-", "");
        name = name.length() > MAX_LENGTH_OF_NAME_SLICE ? name.substring(0, MAX_LENGTH_OF_NAME_SLICE) : name;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            String storageAccountId = acv.getId().toString() + "#" + cloudContext.getId() + "#" + cloudContext.getOwner();
            LOGGER.info("Storage account internal id: {}", storageAccountId);
            byte[] digest = messageDigest.digest(storageAccountId.getBytes());
            String paddedId = "";
            if (armAttachedStorageOption == ArmAttachedStorageOption.PER_VM && vmId != null) {
                paddedId = String.format("%3s", Long.toString(vmId, RADIX)).replace(' ', '0');
            }
            result = name + storageType.getAbbreviation() + paddedId + new BigInteger(1, digest).toString(RADIX);
        } catch (NoSuchAlgorithmException e) {
            result = name + acv.getId() + cloudContext.getId() + cloudContext.getOwner();
        }
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME);
        }
        LOGGER.info("Storage account name: {}", result);
        return result;
    }

    private String getPersistentStorageName(String persistentStorageName, ArmCredentialView acv, String region) {
        String subscriptionIdPart = acv.getSubscriptionId().replaceAll("-", "").toLowerCase();
        String regionInitials = WordUtils.initials(region, ' ').toLowerCase();
        String result = String.format("%s%s%s", persistentStorageName, regionInitials, subscriptionIdPart);
        if (result.length() > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME);
        }
        LOGGER.info("Storage account name: {}", result);
        return result;
    }

    public String getDiskContainerName(CloudContext cloudContext) {
        return armUtils.getStackName(cloudContext);
    }

    public String getPersistentStorageName(Map<String, String> parameters) {
        return parameters.get("persistentStorage");
    }

    public boolean isPersistentStorage(String persistentStorageName) {
        return !Strings.isNullOrEmpty(persistentStorageName);
    }

    public String getImageResourceGroupName(CloudContext cloudContext, Map<String, String> parameters) {
        if (isPersistentStorage(getPersistentStorageName(parameters))) {
            return getPersistentStorageName(parameters);
        }
        return armUtils.getResourceGroupName(cloudContext);
    }

    private boolean storageAccountExist(AzureRMClient client, String storageName) {
        try {
            List<Map<String, Object>> storageAccounts = client.getStorageAccounts();
            for (Map<String, Object> stringObjectMap : storageAccounts) {
                if (stringObjectMap.get("name").equals(storageName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}


