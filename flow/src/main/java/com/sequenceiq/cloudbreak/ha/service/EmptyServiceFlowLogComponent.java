package com.sequenceiq.cloudbreak.ha.service;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class EmptyServiceFlowLogComponent implements ServiceFlowLogComponent {
    @Override
    public int purgeTerminatedResourceLogs() {
        return 0;
    }

    @Override
    public Set<Long> findTerminatingResourcesByNodeId(String nodeId) {
        return Set.of();
    }
}
