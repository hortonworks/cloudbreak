package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class AzureInstanceView {

    private final CloudInstance instance;

    private final InstanceTemplate instanceTemplate;

    private final int stackNamePrefixLength;

    private final InstanceGroupType type;

    private final String attachedDiskStorage;

    private final String attachedDiskStorageType;

    private final String groupName;

    private final String stackName;

    private final String availabilitySetName;

    private final boolean managedDisk;

    private final String subnetId;

    private final int rootVolumeSize;

    private final String customImageId;

    public AzureInstanceView(String stackName, int stackNamePrefixLength, CloudInstance instance, InstanceGroupType type, String attachedDiskStorage,
            String attachedDiskStorageType, String groupName, String availabilitySetName, boolean managedDisk, String subnetId, int rootVolumeSize,
            String customImageId) {
        this.instance = instance;
        instanceTemplate = instance.getTemplate();
        this.stackNamePrefixLength = stackNamePrefixLength;
        this.type = type;
        this.attachedDiskStorage = attachedDiskStorage;
        this.attachedDiskStorageType = attachedDiskStorageType;
        this.groupName = groupName;
        this.stackName = stackName;
        this.availabilitySetName = availabilitySetName;
        this.managedDisk = managedDisk;
        this.subnetId = subnetId;
        this.rootVolumeSize = rootVolumeSize;
        this.customImageId = customImageId;
    }

    /**
     * Used in freemarker template.
     */
    public String getHostName() {
        String hostName = instance.getStringParameter(CloudInstance.DISCOVERY_NAME);
        return hostName == null ? getInstanceName() : hostName;
    }

    /**
     * Used in freemarker template.
     */
    public String getInstanceName() {
        String instanceName = instance.getStringParameter(CloudInstance.INSTANCE_NAME);
        if (instanceName != null) {
            return instanceName;
        } else {
            String shortenedStackName = stackName.length() > stackNamePrefixLength ? stackName.substring(0, stackNamePrefixLength) : stackName;
            return shortenedStackName + '-' + getInstanceId();
        }
    }

    /**
     * Used in freemarker template.
     */
    public String getFlavor() {
        return instanceTemplate.getFlavor();
    }

    /**
     * Used in OLD freemarker template -> backward compatibility
     */
    public boolean isBootDiagnosticsEnabled() {
        return AzureDiskType.LOCALLY_REDUNDANT.equals(AzureDiskType.getByValue(instanceTemplate.getVolumes().get(0).getType()));
    }

    public InstanceGroupType getType() {
        return type;
    }

    public String getInstanceId() {
        return AzureUtils.getGroupName(instanceTemplate.getGroupName()) + instanceTemplate.getPrivateId();
    }

    public long getPrivateId() {
        return instanceTemplate.getPrivateId();
    }

    public List<AzureVolumeView> getVolumes() {
        List<AzureVolumeView> list = new ArrayList<>();
        int index = 0;
        for (Volume volume : instanceTemplate.getVolumes()) {
            AzureVolumeView cv = new AzureVolumeView(volume, index);
            list.add(cv);
            index++;
        }
        return list;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isManagedDisk() {
        return managedDisk;
    }

    public String getAttachedDiskStorageName() {
        return attachedDiskStorage;
    }

    public String getAttachedDiskStorageType() {
        return attachedDiskStorageType;
    }

    /**
     * Used in freemarker template.
     */
    public String getAttachedDiskStorageUrl() {
        return String.format(AzureStorage.STORAGE_BLOB_PATTERN, attachedDiskStorage);
    }

    public String getMetadata() {
        try {
            return JsonUtil.writeValueAsString(generateMetadata());
        } catch (JsonProcessingException ignored) {
            return generateMetadata().toString();
        }
    }

    private Map<String, String> generateMetadata() {
        return new HashMap<>();
    }

    public String getAvailabilitySetName() {
        return availabilitySetName;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public int getRootVolumeSize() {
        return rootVolumeSize;
    }

    public String getCustomImageId() {
        return customImageId;
    }
}