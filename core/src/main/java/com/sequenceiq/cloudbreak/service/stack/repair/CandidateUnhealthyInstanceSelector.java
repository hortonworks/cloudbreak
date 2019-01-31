package com.sequenceiq.cloudbreak.service.stack.repair;

import static java.lang.String.format;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
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
        Set<InstanceMetaData> candidateUnhealthyInstances = new HashSet<>();
        hostStatuses.keySet().stream().filter(hostName -> hostName != null && "UNKNOWN".equals(hostStatuses.get(hostName))).forEach(hostName -> {
            InstanceMetaData instanceMetaData = instanceMetaDataService.findHostInStack(stackId, hostName)
                    .orElseThrow(() -> NotFoundException.notFound("InstanceMetadata", format("%s, %s", stackId, hostName)).get());
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
