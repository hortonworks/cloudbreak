package com.sequenceiq.cloudbreak.ha.service;

import java.util.Set;

public interface ServiceFlowLogComponent {
    int purgeTerminatedResourceLogs();

    Set<Long> findTerminatingResourcesByNodeId(String nodeId);
}
