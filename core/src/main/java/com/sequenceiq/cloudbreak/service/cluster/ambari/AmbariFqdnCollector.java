package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class AmbariFqdnCollector {

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public Map<String, List<String>> collectFqdns(Stack stack) {
        Map<String, List<String>> result = new HashMap<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            result.put(
                    instanceGroup.getGroupName(),
                    instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroup.getId())
                            .stream()
                            .map(InstanceMetaData::getDiscoveryFQDN)
                            .collect(Collectors.toList()));
        }
        return result;
    }
}
