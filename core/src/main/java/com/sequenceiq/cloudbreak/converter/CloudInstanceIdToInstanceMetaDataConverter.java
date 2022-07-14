package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class CloudInstanceIdToInstanceMetaDataConverter {

    public List<InstanceMetadataView> getNotDeletedAndNotZombieInstances(List<InstanceMetadataView> instanceMetadataViews, String hostGroupname,
            Set<String> cloudInstanceIds) {
        List<InstanceMetadataView> instanceMetadataViewsInGroup = instanceMetadataViews
                .stream().filter(ig -> ig.getInstanceGroupName().equals(hostGroupname))
                .collect(Collectors.toList());
        if (instanceMetadataViewsInGroup.isEmpty()) {
            return new ArrayList<>();
        }
        return instanceMetadataViewsInGroup.stream()
                .filter(im -> im.getInstanceId() == null ? false : cloudInstanceIds.contains(im.getInstanceId()))
                .collect(Collectors.toList());
    }

}
