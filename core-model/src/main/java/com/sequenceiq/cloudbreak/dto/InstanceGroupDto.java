package com.sequenceiq.cloudbreak.dto;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public class InstanceGroupDto {

    private final InstanceGroupView instanceGroup;

    private final List<InstanceMetadataView> instanceMetadataViews;

    public InstanceGroupDto(InstanceGroupView instanceGroup, List<InstanceMetadataView> instanceMetadataViews) {
        this.instanceGroup = instanceGroup;
        this.instanceMetadataViews = instanceMetadataViews;
    }

    public InstanceGroupView getInstanceGroup() {
        return instanceGroup;
    }

    public List<InstanceMetadataView> getInstanceMetadataViews() {
        return instanceMetadataViews;
    }

    public List<InstanceMetadataView> getNotDeletedAndNotZombieInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider() && !metaData.isZombie())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getNotTerminateAndNotZombieInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isZombie())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getNotDeletedInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getReachableInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> metaData.isReachable())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getReachableOrStoppedInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> metaData.isReachableOrStopped())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getRunningInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(InstanceMetadataView::isRunning)
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getDeletedInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> metaData.isDeletedOnProvider())
                .collect(Collectors.toList());
    }

    public int getNodeCount() {
        return instanceMetadataViews.size();
    }

    public List<InstanceMetadataView> getUnattachedInstanceMetaData() {
        return instanceMetadataViews.stream()
                .filter(metaData -> metaData.getInstanceStatus() == InstanceStatus.CREATED)
                .collect(Collectors.toList());
    }

    public void addAllInstanceMetadata(Collection<InstanceMetaData> instanceMetaData) {
        instanceMetaData.forEach(im -> addInstanceMetadata(im));
    }

    public void addInstanceMetadata(InstanceMetaData instanceMetaData) {
        // TODO: CB-16970 until we use the mix DTO and entities, we need to handle as backward compatibility
        if (instanceGroup instanceof InstanceGroup) {
            ((InstanceGroup) instanceGroup).getAllInstanceMetaData().add(instanceMetaData);
        }
        instanceMetadataViews.add(instanceMetaData);
    }
}
