package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.arm.ArmDiskType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class ArmStackView {

    private Map<String, List<ArmInstanceView>> groups = new HashMap<>();

    private List<String> instanceGroups = new ArrayList<>();

    public ArmStackView(String stackName, List<Group> groupList, ArmStorageView armStorageView) {
        for (Group group : groupList) {
            String groupName = group.getType().name();
            List<ArmInstanceView> existingInstances = groups.get(groupName);
            if (existingInstances == null) {
                existingInstances = new ArrayList<>();
                groups.put(groupName, existingInstances);
            }
            for (CloudInstance instance : group.getInstances()) {
                InstanceTemplate template = instance.getTemplate();
                String attachedDiskStorageName = armStorageView.getAttachedDiskStorageName(template);
                ArmInstanceView azureInstance = new ArmInstanceView(stackName, instance, group.getType(), attachedDiskStorageName,
                        template.getVolumeType(), group.getName());
                existingInstances.add(azureInstance);
            }
            instanceGroups.add(group.getName());
        }
    }

    public Map<String, List<ArmInstanceView>> getGroups() {
        return groups;
    }

    public List<String> getInstanceGroups() {
        return instanceGroups;
    }

    public Map<String, ArmDiskType> getStorageAccounts() {
        Map<String, ArmDiskType> storageAccounts = new HashMap<>();
        for (List<ArmInstanceView> list : getGroups().values()) {
            for (ArmInstanceView armInstanceView : list) {
                storageAccounts.put(armInstanceView.getAttachedDiskStorageName(), ArmDiskType.getByValue(armInstanceView.getAttachedDiskStorageType()));
            }
        }
        return storageAccounts;
    }
}