package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class AzureInstanceView {

    private CloudInstance instance;

    private InstanceTemplate instanceTemplate;

    private int stackNamePrefixLength;

    private InstanceGroupType type;

    private String attachedDiskStorage;

    private String attachedDiskStorageType;

    private String groupName;

    private String stackName;

    private String availabilitySetName;

    private boolean managedDisk;

    public AzureInstanceView(String stackName, int stackNamePrefixLength, CloudInstance instance, InstanceGroupType type, String attachedDiskStorage,
            String attachedDiskStorageType, String groupName, String availabilitySetName, boolean managedDisk) {
        this.instance = instance;
        this.instanceTemplate = instance.getTemplate();
        this.stackNamePrefixLength = stackNamePrefixLength;
        this.type = type;
        this.attachedDiskStorage = attachedDiskStorage;
        this.attachedDiskStorageType = attachedDiskStorageType;
        this.groupName = groupName;
        this.stackName = stackName;
        this.availabilitySetName = availabilitySetName;
        this.managedDisk = managedDisk;
    }

    /**
     * Used in freemarker template.
     */
    public String getHostName() {
        String hostName = instance.getStringParameter(CloudInstance.DISCOVERY_NAME);
        if (hostName == null) {
            String shortenedStackname;
            if (stackName.length() > stackNamePrefixLength) {
                shortenedStackname = stackName.substring(0, stackNamePrefixLength);
            } else {
                shortenedStackname = stackName;
            }
            hostName = shortenedStackname + "-" + getInstanceId();
        }
        return hostName;
    }

    /**
     * Used in freemarker template.
     */
    public String getFlavor() {
        return instanceTemplate.getFlavor();
    }

    /**
     * Used in freemarker template.
     */
    public boolean isBootDiagnosticsEnabled() {
        return AzureDiskType.LOCALLY_REDUNDANT.equals(AzureDiskType.getByValue(instanceTemplate.getVolumeType()));
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
        } catch (JsonProcessingException e) {
            return generateMetadata().toString();
        }
    }

    private Map<String, String> generateMetadata() {
        return new HashMap<>();
    }

    public String getAvailabilitySetName() {
        return availabilitySetName;
    }
}