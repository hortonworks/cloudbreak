package com.sequenceiq.cloudbreak.ha.service;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class EmptyServiceFlowLogComponent implements ServiceFlowLogComponent {
    @Override
    public int purgeTerminatedStackLogs() {
        return 0;
    }

    @Override
    public Set<Long> findTerminatingStacksByCloudbreakNodeId(String cloudbreakNodeId) {
        return Set.of();
    }
}
