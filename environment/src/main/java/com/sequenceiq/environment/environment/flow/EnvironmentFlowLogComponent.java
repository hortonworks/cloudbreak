package com.sequenceiq.environment.environment.flow;

import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ha.service.ServiceFlowLogComponent;

@Primary
@Component
public class EnvironmentFlowLogComponent implements ServiceFlowLogComponent {

    private final EnvironmentFlowLogRepository flowLogRepository;

    public EnvironmentFlowLogComponent(EnvironmentFlowLogRepository flowLogRepository) {
        this.flowLogRepository = flowLogRepository;
    }

    @Override
    public int purgeTerminatedResourceLogs() {
        return flowLogRepository.purgeArchivedEnvironmentLogs();
    }

    @Override
    public Set<Long> findTerminatingResourcesByNodeId(String nodeId) {
        return flowLogRepository.findPendingResourcesByNodeId(nodeId);
    }
}
