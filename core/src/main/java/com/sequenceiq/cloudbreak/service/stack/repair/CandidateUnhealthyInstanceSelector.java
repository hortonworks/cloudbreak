package com.sequenceiq.cloudbreak.service.stack.repair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class CandidateUnhealthyInstanceSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CandidateUnhealthyInstanceSelector.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public Set<InstanceMetaData> selectCandidateUnhealthyInstances(long stackId) {
        Map<String, String> hostStatuses = clusterService.getHostStatuses(stackId);
        LOGGER.debug("HostStatuses: {}", hostStatuses);
        Set<InstanceMetaData> candidateUnhealthyInstances = new HashSet<>();
        hostStatuses.keySet().stream().filter(hostName -> hostName != null && "UNKNOWN".equals(hostStatuses.get(hostName))).forEach(hostName -> {
            InstanceMetaData instanceMetaData = instanceMetaDataRepository.findHostInStack(stackId, hostName);
            if (isAWorker(instanceMetaData)) {
                candidateUnhealthyInstances.add(instanceMetaData);
            }
        });
        LOGGER.debug("Candidate Unhealthy Instances: {}", candidateUnhealthyInstances);
        return candidateUnhealthyInstances;
    }

    private boolean isAWorker(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceGroup().getInstanceGroupType().equals(InstanceGroupType.CORE);
    }

}
