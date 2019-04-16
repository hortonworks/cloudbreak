package com.sequenceiq.cloudbreak.service.stack.repair;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class CandidateUnhealthyInstanceSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CandidateUnhealthyInstanceSelector.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public Set<InstanceMetaData> selectCandidateUnhealthyInstances(long stackId) {
        Map<String, String> hostStatuses = clusterService.getHostStatuses(stackId);
        LOGGER.debug("HostStatuses: {}", hostStatuses);
        Set<InstanceMetaData> candidateUnhealthyInstances = hostStatuses.entrySet().stream()
                .filter(entry -> isUnhealthyStatus(entry.getValue()))
                .map(Map.Entry::getKey)
                .filter(Objects::nonNull)
                .map(hostName -> instanceMetaDataService.findHostInStack(stackId, hostName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(CandidateUnhealthyInstanceSelector::isAWorker)
                .collect(Collectors.toSet());
        LOGGER.debug("Candidate Unhealthy Instances: {}", candidateUnhealthyInstances);
        return candidateUnhealthyInstances;
    }

    private static boolean isUnhealthyStatus(String status) {
        // FIXME "UNKNOWN" is Ambari-specific
        return "UNKNOWN".equals(status);
    }

    private static boolean isAWorker(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceGroup().getInstanceGroupType().equals(InstanceGroupType.CORE);
    }

}
