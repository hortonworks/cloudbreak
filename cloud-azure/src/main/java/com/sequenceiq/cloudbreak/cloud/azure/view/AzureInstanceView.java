package com.sequenceiq.cloudbreak.cloud.azure.view;

import static java.util.Objects.requireNonNull;

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
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;

public final class AzureInstanceView {

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

    private final String managedIdentity;

    private AzureInstanceView(Builder builder) {
        instance = builder.instance;
        instanceTemplate = instance.getTemplate();
        stackNamePrefixLength = builder.stackNamePrefixLength;
        type = builder.type;
        attachedDiskStorage = builder.attachedDiskStorage;
        attachedDiskStorageType = builder.attachedDiskStorageType;
        groupName = builder.groupName;
        stackName = builder.stackName;
        availabilitySetName = builder.availabilitySetName;
        managedDisk = builder.managedDisk;
        subnetId = builder.subnetId;
        rootVolumeSize = builder.rootVolumeSize;
        customImageId = builder.customImageId;
        managedIdentity = builder.managedIdentity;
    }

    public CloudInstance getInstance() {
        return instance;
    }

    public boolean hasRealInstanceId() {
        return instance.getInstanceId() != null;
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
     * Used in OLD freemarker template {@literal ->} backward compatibility
     */
    public boolean isBootDiagnosticsEnabled() {
        return AzureDiskType.LOCALLY_REDUNDANT.equals(AzureDiskType.getByValue(instanceTemplate.getVolumes().get(0).getType()));
    }

    public InstanceGroupType getType() {
        return type;
    }

    // It is not an instance ID, it's a suffix, looks like 'm8-1b2c3c5'
    public String getInstanceId() {
        String instanceName = instance.getStringParameter(CloudInstance.INSTANCE_NAME);
        if (instanceName != null) {
            return instanceName.replaceAll(stackName + "-", "");
        } else {
            String id = instance.getDbIdOrDefaultIfNotExists();
            return AzureUtils.getInstanceIdWithoutStackName(instanceTemplate.getGroupName(), instanceTemplate.getPrivateId().toString(), id);
        }
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

    public String getManagedIdentity() {
        return managedIdentity;
    }

    public boolean isManagedDiskEncryptionWithCustomKeyEnabled() {
        Object encryptedWithCustomKey = instanceTemplate.getParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Object.class);
        if (encryptedWithCustomKey instanceof Boolean) {
            return (Boolean) encryptedWithCustomKey;
        } else if (encryptedWithCustomKey instanceof String) {
            return Boolean.parseBoolean((String) encryptedWithCustomKey);
        }
        return false;
    }

    public boolean isHostEncryptionEnabled() {
        Boolean hostEncryptionEnabled = instanceTemplate.getParameter(AzureInstanceTemplate.ENCRYPTION_AT_HOST_ENABLED, Boolean.class);
        if (hostEncryptionEnabled == null) {
            return false;
        } else {
            return hostEncryptionEnabled;
        }
    }

    public String getDiskEncryptionSetId() {
        return instanceTemplate.getStringParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID);
    }

    public static Builder builder(CloudInstance instance) {
        return new Builder(instance);
    }

    public static class Builder {

        private CloudInstance instance;

        private String stackName;

        private int stackNamePrefixLength;

        private InstanceGroupType type;

        private String attachedDiskStorage;

        private String attachedDiskStorageType;

        private String groupName;

        private String availabilitySetName;

        private boolean managedDisk;

        private String subnetId;

        private int rootVolumeSize;

        private String customImageId;

        private String managedIdentity;

        private Builder(CloudInstance instance) {
            withInstance(instance);
        }

        public Builder withInstance(CloudInstance instance) {
            this.instance = requireNonNull(instance);
            return this;
        }

        public Builder withStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder withStackNamePrefixLength(int stackNamePrefixLength) {
            this.stackNamePrefixLength = stackNamePrefixLength;
            return this;
        }

        public Builder withType(InstanceGroupType type) {
            this.type = type;
            return this;
        }

        public Builder withAttachedDiskStorage(String attachedDiskStorage) {
            this.attachedDiskStorage = attachedDiskStorage;
            return this;
        }

        public Builder withAttachedDiskStorageType(String attachedDiskStorageType) {
            this.attachedDiskStorageType = attachedDiskStorageType;
            return this;
        }

        public Builder withGroupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder withAvailabilitySetName(String availabilitySetName) {
            this.availabilitySetName = availabilitySetName;
            return this;
        }

        public Builder withManagedDisk(boolean managedDisk) {
            this.managedDisk = managedDisk;
            return this;
        }

        public Builder withSubnetId(String subnetId) {
            this.subnetId = subnetId;
            return this;
        }

        public Builder withRootVolumeSize(int rootVolumeSize) {
            this.rootVolumeSize = rootVolumeSize;
            return this;
        }

        public Builder withCustomImageId(String customImageId) {
            this.customImageId = customImageId;
            return this;
        }

        public Builder withManagedIdentity(String managedIdentity) {
            this.managedIdentity = managedIdentity;
            return this;
        }

        public AzureInstanceView build() {
            return new AzureInstanceView(this);
        }

    }

}