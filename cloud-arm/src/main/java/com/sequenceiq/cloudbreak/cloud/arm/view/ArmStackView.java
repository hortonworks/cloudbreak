package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class ArmStackView {

    private Map<String, List<ArmInstanceView>> groups = new HashMap<>();

    public ArmStackView(List<Group> groupList, ArmStorageView armStorageView) {
        for (Group group : groupList) {
            String groupName = group.getType().name();
            List<ArmInstanceView> existingInstances = groups.get(groupName);
            if (existingInstances == null) {
                existingInstances = new ArrayList<>();
                groups.put(groupName, existingInstances);
            }
            for (CloudInstance instance : group.getInstances()) {
                ArmInstanceView azureInstance = new ArmInstanceView(instance.getTemplate(), group.getType(), armStorageView);
                existingInstances.add(azureInstance);
            }
        }
    }

    public Map<String, List<ArmInstanceView>> getGroups() {
        return groups;
    }

    public Set<String> getStorageAccountNames() {
        Set<String> storageAccountNames = new HashSet<>();
        for (List<ArmInstanceView> list : getGroups().values()) {
            for (ArmInstanceView armInstanceView : list) {
                storageAccountNames.add(armInstanceView.getAttachedDiskStorageName());
            }
        }
        return storageAccountNames;
    }
}