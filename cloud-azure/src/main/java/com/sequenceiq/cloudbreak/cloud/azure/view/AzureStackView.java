package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;

public class AzureStackView {

    private static final int DEFAULT_FAULT_DOMAIN_COUNTER = 2;

    private static final int DEFAULT_UPDATE_DOMAIN_COUNTER = 20;

    private final Map<String, List<AzureInstanceView>> instancesByGroupType = new HashMap<>();

    private final List<AzureInstanceGroupView> instanceGroups = new ArrayList<>();

    private final List<String> instanceGroupNames = new ArrayList<>();

    public AzureStackView(String stackName, int stackNamePrefixLength, Iterable<Group> groupList, AzureStorageView armStorageView,
            AzureSubnetStrategy subnetStrategy, Map<String, String> customImageNamePerInstance) {
        for (Group group : groupList) {
            String groupType = group.getType().name();
            AzureInstanceGroupView instanceGroupView;
            Map<?, ?> asMap = group.getParameter("availabilitySet", HashMap.class);
            if (asMap != null) {
                String asName = (String) asMap.get("name");
                Integer faultDomainCount = asMap.get("faultDomainCount") != null
                        ? (Integer) asMap.get("faultDomainCount") : DEFAULT_FAULT_DOMAIN_COUNTER;
                Integer updateDomainCount = asMap.get("updateDomainCount") != null
                        ? (Integer) asMap.get("updateDomainCount") : DEFAULT_UPDATE_DOMAIN_COUNTER;

                instanceGroupView = new AzureInstanceGroupView(group.getName(), faultDomainCount, updateDomainCount,
                        asName, group.getRootVolumeSize());
            } else {
                instanceGroupView = new AzureInstanceGroupView(group.getName(), group.getRootVolumeSize());
            }
            if (!group.getInstances().isEmpty()) {
                List<AzureInstanceView> existingInstancesForGroupType = instancesByGroupType.computeIfAbsent(groupType, k -> new ArrayList<>());
                for (CloudInstance instance : group.getInstances()) {
                    InstanceTemplate template = instance.getTemplate();
                    String attachedDiskStorageName = armStorageView.getAttachedDiskStorageName(template);
                    String attachedDiskStorageType = template.getVolumes().isEmpty() ? AzurePlatformParameters.defaultDiskType().value()
                            : template.getVolumes().get(0).getType();
                    AzureInstanceView azureInstance = AzureInstanceView.builder(instance)
                            .withStackName(stackName)
                            .withStackNamePrefixLength(stackNamePrefixLength)
                            .withType(group.getType())
                            .withAttachedDiskStorage(attachedDiskStorageName)
                            .withAttachedDiskStorageType(attachedDiskStorageType)
                            .withGroupName(group.getName())
                            .withAvailabilitySetName(instanceGroupView.getAvailabilitySetName())
                            .withManagedDisk(true)
                            .withSubnetId(getInstanceSubnetId(instance, subnetStrategy))
                            .withRootVolumeSize(group.getRootVolumeSize())
                            .withCustomImageId(!customImageNamePerInstance.isEmpty() ? customImageNamePerInstance.get(instance.getInstanceId()) : null)
                            .withManagedIdentity(getManagedIdentity(group))
                            .build();
                    existingInstancesForGroupType.add(azureInstance);
                }
                instanceGroupView.setManagedDisk(true);
            }

            instanceGroupNames.add(group.getName());
            instanceGroups.add(instanceGroupView);
        }
    }

    public Map<String, List<AzureInstanceView>> getInstancesByGroupType() {
        return instancesByGroupType;
    }

    public List<AzureInstanceGroupView> getInstanceGroups() {
        return instanceGroups;
    }

    public List<String> getInstanceGroupNames() {
        return instanceGroupNames;
    }

    private String getInstanceSubnetId(CloudInstance instance, AzureSubnetStrategy subnetStrategy) {
        String stored = instance.getSubnetId();
        if (StringUtils.isNoneBlank(stored)) {
            return stored;
        }
        return subnetStrategy.getNextSubnetId();
    }

    private String getManagedIdentity(Group group) {
        return group.getIdentity().map(cloudFileSystemView -> {
            CloudAdlsGen2View cloudAdlsGen2View = (CloudAdlsGen2View) cloudFileSystemView;
            return cloudAdlsGen2View.getManagedIdentity();
        }).orElse(null);
    }
}