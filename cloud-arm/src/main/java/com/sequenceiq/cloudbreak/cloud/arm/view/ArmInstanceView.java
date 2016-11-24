package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.arm.ArmDiskType;
import com.sequenceiq.cloudbreak.cloud.arm.ArmStorage;
import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class ArmInstanceView {

    private CloudInstance instance;

    private InstanceTemplate instanceTemplate;

    private InstanceGroupType type;

    private String attachedDiskStorage;

    private String attachedDiskStorageType;

    private String groupName;

    private String stackName;

    public ArmInstanceView(String stackName, CloudInstance instance, InstanceGroupType type, String attachedDiskStorage,
            String attachedDiskStorageType, String groupName) {
        this.instance = instance;
        this.instanceTemplate = instance.getTemplate();
        this.type = type;
        this.attachedDiskStorage = attachedDiskStorage;
        this.attachedDiskStorageType = attachedDiskStorageType;
        this.groupName = groupName;
        this.stackName = stackName;
    }

    /**
     * Used in freemarker template.
     */
    public String getHostName() {
        String hostName = instance.getStringParameter(CloudInstance.DISCOVERY_NAME);
        if (hostName == null) {
            hostName = stackName + "-" + getInstanceId();
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
        return ArmDiskType.LOCALLY_REDUNDANT.equals(ArmDiskType.getByValue(instanceTemplate.getVolumeType()));
    }

    public InstanceGroupType getType() {
        return type;
    }

    public String getInstanceId() {
        return ArmUtils.getGroupName(instanceTemplate.getGroupName()) + instanceTemplate.getPrivateId();
    }

    public long getPrivateId() {
        return instanceTemplate.getPrivateId();
    }

    public List<ArmVolumeView> getVolumes() {
        List<ArmVolumeView> list = new ArrayList<>();
        int index = 0;
        for (Volume volume : instanceTemplate.getVolumes()) {
            ArmVolumeView cv = new ArmVolumeView(volume, index);
            list.add(cv);
            index++;
        }
        return list;
    }

    public String getGroupName() {
        return groupName;
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
        return String.format(ArmStorage.STORAGE_BLOB_PATTERN, attachedDiskStorage);
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
}