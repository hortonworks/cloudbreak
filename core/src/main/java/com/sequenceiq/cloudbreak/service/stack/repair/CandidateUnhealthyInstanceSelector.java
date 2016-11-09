package com.sequenceiq.cloudbreak.service.stack.repair;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CandidateUnhealthyInstanceSelector {

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public Set<InstanceMetaData> selectCandidateUnhealthyInstances(Stack stack)
            throws CloudbreakSecuritySetupException {
        Map<String, String> hostStatuses = clusterService.getHostStatuses(stack.getId());
        Set<InstanceMetaData> candidateUnhealthyInstances = new HashSet<>();
        hostStatuses.keySet().stream().filter(hostName -> hostStatuses.get(hostName).equals("UNKNOWN")).forEach(hostName -> {
            InstanceMetaData instanceMetaData = instanceMetaDataRepository.findHostInStack(stack.getId(), hostName);
            if (isAWorker(instanceMetaData)) {
                candidateUnhealthyInstances.add(instanceMetaData);
            }
        });
        return candidateUnhealthyInstances;
    }

    private boolean isAWorker(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceGroup().getInstanceGroupType().equals(InstanceGroupType.CORE);
    }

}
