package com.sequenceiq.cloudbreak.service.stack.repair;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class CandidateUnhealthyInstanceSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CandidateUnhealthyInstanceSelector.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public Set<InstanceMetadataView> selectCandidateUnhealthyInstances(long stackId) {
        Map<HostName, String> hostStatuses = clusterService.getHostStatuses(stackId);
        LOGGER.debug("HostStatuses: {}", hostStatuses);
        List<String> hostnames = hostStatuses.entrySet().stream()
                .filter(entry -> isUnhealthyStatus(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(HostName::value)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<InstanceMetadataView> candidateUnhealthyInstances = instanceMetaDataService.findAllWorkerWithHostnamesInStack(stackId, hostnames);
        LOGGER.debug("Candidate Unhealthy Instances: {}", candidateUnhealthyInstances);
        return new HashSet<>(candidateUnhealthyInstances);
    }

    private static boolean isUnhealthyStatus(String status) {
        // FIXME "UNKNOWN" is Ambari-specific
        return "UNKNOWN".equals(status);
    }
}
