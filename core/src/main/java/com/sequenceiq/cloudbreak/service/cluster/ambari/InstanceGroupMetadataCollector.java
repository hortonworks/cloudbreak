package com.sequenceiq.cloudbreak.service.cluster.ambari;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InstanceGroupMetadataCollector {

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public Map<String, List<InstanceMetaData>> collectMetadata(Stack stack) {
        Map<String, List<InstanceMetaData>> result = new HashMap<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            result.put(
                    instanceGroup.getGroupName(),
                    instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroup.getId())
                            .stream()
                            .collect(Collectors.toList()));
        }
        return result;
    }
}
