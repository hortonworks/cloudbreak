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

    private static final int DEFAULT_FAULT_DOMAIN_COUNTER = 3;

    private static final int DEFAULT_UPDATE_DOMAIN_COUNTER = 20;

    private Map<String, List<AzureInstanceView>> groups = new HashMap<>();

    private List<AzureInstanceGroupView> instanceGroups = new ArrayList<>();

    private List<String> instanceGroupNames = new ArrayList<>();

    public AzureStackView(String stackName, int stackNamePrefixLength, List<Group> groupList, AzureStorageView armStorageView) {
        for (Group group : groupList) {
            String groupName = group.getType().name();
            List<AzureInstanceView> existingInstances = groups.get(groupName);
            if (existingInstances == null) {
                existingInstances = new ArrayList<>();
                groups.put(groupName, existingInstances);
            }
            AzureInstanceGroupView instanceGroupView;
            Map asMap = group.getParameter("availabilitySet", HashMap.class);
            if (asMap != null) {
                String asName = (String) asMap.get("name");
                Integer faultDomainCount = (asMap != null && asMap.get("faultDomainCount") != null)
                        ? (Integer) asMap.get("faultDomainCount") : DEFAULT_FAULT_DOMAIN_COUNTER;
                Integer updateDomainCount = (asMap != null && asMap.get("updateDomainCount") != null)
                        ? (Integer) asMap.get("updateDomainCount") : DEFAULT_UPDATE_DOMAIN_COUNTER;

                instanceGroupView = new AzureInstanceGroupView(group.getName(), faultDomainCount, updateDomainCount,
                        asName);
            } else {
                instanceGroupView = new AzureInstanceGroupView(group.getName());
            }
            for (CloudInstance instance : group.getInstances()) {
                InstanceTemplate template = instance.getTemplate();
                String attachedDiskStorageName = armStorageView.getAttachedDiskStorageName(template);
                boolean managedDisk = Boolean.TRUE.equals(instance.getTemplate().getParameter("managedDisk", Boolean.class));
                AzureInstanceView azureInstance = new AzureInstanceView(stackName, stackNamePrefixLength, instance, group.getType(), attachedDiskStorageName,
                        template.getVolumeType(), group.getName(), instanceGroupView.getAvailabilitySetName(), managedDisk);
                existingInstances.add(azureInstance);
            }
            instanceGroupNames.add(group.getName());
            instanceGroups.add(instanceGroupView);
        }
    }

    public Map<String, List<AzureInstanceView>> getGroups() {
        return groups;
    }

    public List<AzureInstanceGroupView> getInstanceGroups() {
        return instanceGroups;
    }

    public List<String> getInstanceGroupNames() {
        return instanceGroupNames;
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