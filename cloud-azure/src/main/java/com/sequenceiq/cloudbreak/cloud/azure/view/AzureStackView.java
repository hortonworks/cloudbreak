package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AzureStackView {

    private Map<String, List<AzureInstanceView>> groups = new HashMap<>();

    private List<String> instanceGroups = new ArrayList<>();

    public AzureStackView(String stackName, int stackNamePrefixLength, List<Group> groupList, AzureStorageView armStorageView) {
        for (Group group : groupList) {
            String groupName = group.getType().name();
            List<AzureInstanceView> existingInstances = groups.get(groupName);
            if (existingInstances == null) {
                existingInstances = new ArrayList<>();
                groups.put(groupName, existingInstances);
            }
            for (CloudInstance instance : group.getInstances()) {
                InstanceTemplate template = instance.getTemplate();
                String attachedDiskStorageName = armStorageView.getAttachedDiskStorageName(template);
                AzureInstanceView azureInstance = new AzureInstanceView(stackName, stackNamePrefixLength, instance, group.getType(), attachedDiskStorageName,
                        template.getVolumeType(), group.getName());
                existingInstances.add(azureInstance);
            }
            instanceGroups.add(group.getName());
        }
    }

    public Map<String, List<AzureInstanceView>> getGroups() {
        return groups;
    }

    public List<String> getInstanceGroups() {
        return instanceGroups;
    }

    public Map<String, AzureDiskType> getStorageAccounts() {
        Map<String, AzureDiskType> storageAccounts = new HashMap<>();
        for (List<AzureInstanceView> list : getGroups().values()) {
            for (AzureInstanceView armInstanceView : list) {
                storageAccounts.put(armInstanceView.getAttachedDiskStorageName(), AzureDiskType.getByValue(armInstanceView.getAttachedDiskStorageType()));
            }
        }
        return storageAccounts;
    }
}