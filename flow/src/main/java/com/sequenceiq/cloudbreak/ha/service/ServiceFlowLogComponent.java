package com.sequenceiq.cloudbreak.ha.service;

import java.util.Set;

public interface ServiceFlowLogComponent {
    int purgeTerminatedStackLogs();

    Set<Long> findTerminatingStacksByCloudbreakNodeId(String cloudbreakNodeId);
}
